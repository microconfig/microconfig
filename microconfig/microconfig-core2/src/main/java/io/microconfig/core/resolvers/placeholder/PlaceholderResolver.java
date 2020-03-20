package io.microconfig.core.resolvers.placeholder;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.resolvers.RecursiveResolver;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.core.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;

@RequiredArgsConstructor
public class PlaceholderResolver implements RecursiveResolver {
    private final PlaceholderResolveStrategy strategy;

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return findPlaceholderIn(line).map(PlaceholderStatement::new);
    }

    @RequiredArgsConstructor
    private class PlaceholderStatement implements Statement {
        private final PlaceholderBorders borders;

        @Override
        public String resolve(String env, ConfigType configType) {
            Placeholder placeholder = borders.toPlaceholder(configType, env);
            try {
                String maybePlaceholder = placeholder.resolveUsing(strategy);
                return PlaceholderResolver.this.resolve(maybePlaceholder, env, configType);
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