package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Resolver;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;

@RequiredArgsConstructor
public class PlaceholderResolver implements Resolver {
    private final PlaceholderResolveStrategy strategy;

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
        public String resolve() {
            Placeholder placeholder = borders.toPlaceholder("app", "dev");
            return placeholder.resolveUsing(strategy);
        }
    }
}