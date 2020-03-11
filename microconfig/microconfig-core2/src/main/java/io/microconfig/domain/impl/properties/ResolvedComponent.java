package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.ConfigTypeFilter;
import io.microconfig.domain.Property;
import lombok.RequiredArgsConstructor;

import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component original;
    private final PropertyResolver resolveStrategy;

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public String getEnvironment() {
        return original.getEnvironment();
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter configTypes) {
        return original.buildPropertiesFor(configTypes).forEachProperty(resolve());
    }

    private UnaryOperator<Property> resolve() {
        return p -> p.withNewValue(resolveStrategy.resolve(p, getName(), getEnvironment()));
    }
}