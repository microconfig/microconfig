package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.StatementResolver;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;

@RequiredArgsConstructor
public class PlaceholderResolver implements StatementResolver {
    private final PlaceholderResolveStrategy strategy;

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return findPlaceholderIn(line).map(PlaceholderStatement::new);
    }

    @RequiredArgsConstructor
    private class PlaceholderStatement implements Statement {
        private final PlaceholderBorders borders;

        @Override
        public String resolve() {
            Placeholder placeholder = borders.toPlaceholder("app", "dev");
            try {
                String maybePlaceholder = placeholder.resolveUsing(strategy);
                return resolveRecursively(maybePlaceholder);
            } catch (RuntimeException e) {
                String defaultValue = placeholder.getDefaultValue();
                if (defaultValue != null) return defaultValue;
                throw e;
            }
        }

        @Override
        public int getStartIndex() {
            return borders.getStartIndex();
        }

        @Override
        public int getEndIndex() {
            return borders.getEndIndex();
        }
    }
}