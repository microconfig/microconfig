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
import static io.microconfig.core.properties.resolvers.ChainedResolver.chainOf;
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
        return findPlaceholderIn(line).map(b -> new PlaceholderStatement(b, isInsideExpression(line, b.getStartIndex())));
    }

    // an unclosed '#{' before the placeholder: its value becomes a part of the expression, which works with plain text
    private boolean isInsideExpression(CharSequence line, int placeholderStart) {
        int closedBraces = 0;
        for (int i = placeholderStart - 1; i >= 0; i--) {
            char c = line.charAt(i);
            if (c == '}') {
                ++closedBraces;
            } else if (c == '{') {
                if (closedBraces > 0) {
                    --closedBraces;
                } else if (i > 0 && line.charAt(i - 1) == '#') {
                    return true;
                }
            }
        }
        return false;
    }

    @RequiredArgsConstructor
    private class PlaceholderStatement implements Statement {
        private final PlaceholderBorders borders;
        private final boolean insideExpression;

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
                if (defaultValue != null) return defaultValue;
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
            Resolver resolver = chainOf(currentResolverWithVisited(p), expressionResolver);
            String value = resolved.resolveBy(resolver, root).getValue();
            return convertForTarget(value, resolved.getDeclaringComponent(), sourceOfValue);
        }

        // .properties values are kept with their escapes, other values are plain text: text crossing between them is converted
        private String convertForTarget(String value, DeclaringComponent declaredIn, DeclaringComponent target) {
            boolean fromProperties = isPropertiesText(declaredIn);
            boolean toProperties = !insideExpression && isPropertiesText(target);
            if (fromProperties == toProperties) return value;
            return toProperties ? escape(value) : unescape(value);
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
