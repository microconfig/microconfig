package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.ConfigTypeFilter;
import io.microconfig.domain.Property;
import lombok.RequiredArgsConstructor;

import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component component;

    @Override
    public String getName() {
        return component.getName();
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        return component.buildPropertiesFor(filter).forEachProperty(resolve());
    }

    private UnaryOperator<Property> resolve() {
        return p -> p;
    }
}