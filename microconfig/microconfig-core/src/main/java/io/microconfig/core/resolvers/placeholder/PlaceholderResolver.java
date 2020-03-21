package io.microconfig.core.resolvers.placeholder;

import io.microconfig.core.properties.ComponentWithEnv;
import io.microconfig.core.properties.impl.PropertyResolveException;
import io.microconfig.core.resolvers.RecursiveResolver;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.microconfig.core.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.of;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class PlaceholderResolver implements RecursiveResolver {
    private final PlaceholderResolveStrategy strategy;
    private final Set<String> nonOverridableKeys;
    @With(PRIVATE)
    private final Set<Placeholder> visited;

    public PlaceholderResolver(PlaceholderResolveStrategy strategy, Set<String> nonOverridableKeys) {
        this(strategy, nonOverridableKeys, emptySet());
    }

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return findPlaceholderIn(line).map(PlaceholderStatement::new);
    }

    @RequiredArgsConstructor
    private class PlaceholderStatement implements Statement {
        private final PlaceholderBorders borders;

        @Override
        public int getStartIndex() {
            return borders.getStartIndex();
        }

        @Override
        public int getEndIndex() {
            return borders.getEndIndex();
        }

        @Override
        public String resolveFor(ComponentWithEnv sourceOfValue, ComponentWithEnv root) {
            Placeholder placeholder = borders.toPlaceholder(sourceOfValue.getConfigType(), sourceOfValue.getEnvironment());

            return canBeOverridden(placeholder, sourceOfValue) ?
                    overrideByParents(placeholder, sourceOfValue, root) :
                    resolve(placeholder, root);
        }

        private boolean canBeOverridden(Placeholder p, ComponentWithEnv sourceOfValue) {
            return p.isSelfReferenced() ||
                    (p.referencedTo(sourceOfValue) && !nonOverridableKeys.contains(p.getKey()));
        }

        private String overrideByParents(Placeholder p, ComponentWithEnv sourceOfValue, ComponentWithEnv root) {
            Function<ComponentWithEnv, String> tryResolveFor = component -> {
                try {
                    return resolve(p.overrideBy(component), root);
                } catch (RuntimeException e) {
                    return null;
                }
            };

            return of(
                    of(root),
                    visited.stream().map(Placeholder::getReferencedComponent),
                    of(sourceOfValue)
            ).flatMap(identity())
                    .map(tryResolveFor)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> {
                        throw new PropertyResolveException(p.toString(), sourceOfValue, root);
                    });
        }

        private String resolve(Placeholder p, ComponentWithEnv root) {
            try {
                String resolvedValue = p.resolveUsing(strategy);
                return markVisited(p)
                        .resolve(resolvedValue, p.getReferencedComponent(), root);
            } catch (RuntimeException e) {
                String defaultValue = p.getDefaultValue();
                if (defaultValue != null) return defaultValue;
                throw e;
            }
        }

        private PlaceholderResolver markVisited(Placeholder placeholder) {
            Set<Placeholder> updated = new LinkedHashSet<>(visited);
            if (updated.add(placeholder)) {
                return withVisited(unmodifiableSet(updated));
            }

            throw new PropertyResolveException("Found cyclic dependencies:\n" +
                    updated.stream().map(Placeholder::toString).collect(joining(" -> "))
            );
        }
    }
}