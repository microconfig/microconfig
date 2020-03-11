package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.ConfigTypeFilter;
import io.microconfig.domain.Property;
import lombok.RequiredArgsConstructor;

import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component original;

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public String getEnvironment() {
        return original.getEnvironment();
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        return original.buildPropertiesFor(filter).forEachProperty(resolve());
    }

    private UnaryOperator<Property> resolve() {
        return p -> p;
    }
}