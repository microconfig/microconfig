package io.microconfig.core.properties.resolvers.placeholder;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.*;
import io.microconfig.core.properties.resolvers.RecursiveResolver;
import io.microconfig.core.properties.resolvers.expression.ExpressionResolver;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.microconfig.core.properties.io.properties.PropertiesText.escape;
import static io.microconfig.core.properties.io.properties.PropertiesText.isPropertiesText;
import static io.microconfig.core.properties.io.properties.PropertiesText.unescape;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.io.yaml.YamlResolvers.forValues;
import static io.microconfig.core.properties.io.yaml.YamlScalars.isQuoted;
import static io.microconfig.core.properties.io.yaml.YamlScalars.literalValue;
import static io.microconfig.core.properties.io.yaml.YamlScalars.unquote;
import static io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.of;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class PlaceholderResolver implements RecursiveResolver {
    private final EnvironmentRepository environmentRepository;
    private final PlaceholderResolveStrategy strategy;
    private final Set<String> nonOverridableKeys;
    private final RecursiveResolver expressionResolver;
    @With(PRIVATE)
    private final Set<Placeholder> visited;

    public PlaceholderResolver(EnvironmentRepository environmentRepository,
                               PlaceholderResolveStrategy strategy,
                               Set<String> nonOverridableKeys) {
        this(environmentRepository, strategy, nonOverridableKeys, new ExpressionResolver(), emptySet());
    }

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return findPlaceholderIn(line).map(PlaceholderStatement::new);
    }

    @RequiredArgsConstructor
    private class PlaceholderStatement implements Statement {
        private final PlaceholderBorders borders;
        private boolean plainText;

        @Override
        public int getStartIndex() {
            return borders.getStartIndex();
        }

        @Override
        public int getEndIndex() {
            return borders.getEndIndex();
        }

        @Override
        public String resolveFor(DeclaringComponent sourceOfValue, DeclaringComponent root) {
            Placeholder p = borders.toPlaceholder(sourceOfValue.getConfigType(), sourceOfValue.getEnvironment());
            if (hasProfileOfRootEnv(p, root)) {
                p = p.withEnvironment(root.getEnvironment());
            }

            try {
                return canBeOverridden(p, sourceOfValue) ?
                        overrideByParents(p, sourceOfValue, root) :
                        resolve(p, sourceOfValue, root);
            } catch (RuntimeException e) {
                String defaultValue = p.getDefaultValue();
                if (defaultValue != null) {
                    plainText = defaultValue.isEmpty();
                    return defaultValue;
                }
                throw new ResolveException(sourceOfValue, root, "Can't resolve " + this, e);
            }
        }

        private boolean canBeOverridden(Placeholder p, DeclaringComponent sourceOfValue) {
            return p.isSelfReferenced() ||
                    (p.referencedTo(sourceOfValue) && !nonOverridableKeys.contains(p.getKey()));
        }

        private String overrideByParents(Placeholder p, DeclaringComponent sourceOfValue, DeclaringComponent root) {
            Function<DeclaringComponent, String> tryResolveFor = override -> {
                try {
                    return resolve(p.overrideBy(override), sourceOfValue, root);
                } catch (RuntimeException e) {
                    return null;
                }
            };

            return of(
                    of(root),
                    visited.stream().map(Placeholder::getReferencedComponent),
                    of(sourceOfValue)
            ).flatMap(identity())
                    .map(DeclaringComponentImpl::copyOf).distinct()//for correct distinct
                    .map(tryResolveFor)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new ResolveException(sourceOfValue, root, "Can't resolve placeholder " + this));
        }

        private String resolve(Placeholder p, DeclaringComponent sourceOfValue, DeclaringComponent root) {
            Property resolved = p.resolveUsing(strategy, root);
            Resolver resolver = forValues(currentResolverWithVisited(p), expressionResolver);
            String value = resolved.resolveBy(resolver, root).getValue();
            plainText = resolved.getConfigFormat() != YAML;
            return convertForTarget(value, resolved, sourceOfValue);
        }

        // .properties values are kept with their escapes, other values are plain text: text crossing between them is converted
        private String convertForTarget(String value, Property inserted, DeclaringComponent target) {
            boolean fromProperties = isPropertiesText(inserted.getDeclaringComponent());
            boolean toProperties = isPropertiesText(target);
            if (fromProperties && toProperties) return value;

            boolean yamlTarget = target instanceof FileBasedComponent && ((FileBasedComponent) target).isYaml();
            if (inserted.isLiteralText()) {
                if (yamlTarget) return literalValue(value);
                return toProperties ? escape(value) : value;
            }
            String plain = fromProperties ? unescape(value)
                    : !yamlTarget && inserted.getConfigFormat() == YAML && isQuoted(value) ? unquote(value) : value;
            return toProperties ? escape(plain) : plain;
        }

        @Override
        public boolean isPlainText() {
            return plainText;
        }

        private boolean hasProfileOfRootEnv(Placeholder p, DeclaringComponent root) {
            String propEnv = p.getEnvironment();
            String rootEnv = root.getEnvironment();
            if (Objects.equals(propEnv, rootEnv)) return false;
            return environmentRepository.getOrCreateByName(rootEnv)
                    .getProfiles()
                    .contains(propEnv);
        }

        private PlaceholderResolver currentResolverWithVisited(Placeholder placeholder) {
            Set<Placeholder> updated = new LinkedHashSet<>(visited);
            if (updated.add(placeholder)) {
                return withVisited(unmodifiableSet(updated));
            }

            throw new IllegalStateException("Found cyclic dependencies:\n" +
                    updated.stream().map(Placeholder::toString).collect(joining(" -> "))
            );
        }

        @Override
        public String toString() {
            return borders.toString();
        }
    }
}
