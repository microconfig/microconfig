package io.microconfig.core.resolvers.placeholder;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.ComponentDescription;
import io.microconfig.core.resolvers.RecursiveResolver;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.Set;

import static io.microconfig.core.resolvers.placeholder.PlaceholderBorders.findPlaceholderIn;

@RequiredArgsConstructor
public class PlaceholderResolver implements RecursiveResolver {
    private final PlaceholderResolveStrategy strategy;
    private final Set<String> nonOverridableKeys;

    @Override
    public Optional<Statement> findStatementIn(CharSequence line) {
        return findPlaceholderIn(line).map(PlaceholderStatement::new);
    }

    @RequiredArgsConstructor
    private class PlaceholderStatement implements Statement {
        private final PlaceholderBorders borders;

        @Override
        public String resolveFor(ComponentDescription currentComponent,
                                 ComponentDescription rootComponent,
                                 ConfigType configType) {
            //configType, currentComponent, rootComponent
            Placeholder placeholder = borders.toPlaceholder(configType, currentComponent.getEnvironment());
            try {
                String maybePlaceholder = placeholder.resolveUsing(strategy);
                return PlaceholderResolver.this.resolve(maybePlaceholder, currentComponent, rootComponent, configType); //todo incorrect currentComponent can be changed
            } catch (RuntimeException e) {
                String defaultValue = placeholder.getDefaultValue();
                if (defaultValue != null) return defaultValue;
                throw e;
            }

            //dev
            //c1 -> key=${c2@key}
            //c2 -> key=${c3[prod]@key}
            //c3 -> key=${c3@ip}
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