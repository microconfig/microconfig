package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.RequiredArgsConstructor;

import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ResolvableComponent implements Component {
    private final Component component;
    private final Resolver resolver;

    @Override
    public String getName() {
        return component.getName();
    }

    @Override
    public String getEnvironment() {
        return component.getEnvironment();
    }

    @Override
    public ConfigBuildResults getPropertiesFor(ConfigTypeFilter configTypes) {
        return component.getPropertiesFor(configTypes).forEachProperty(resolve());
    }

    private UnaryOperator<Property> resolve() {
        return property -> property.resolveBy(resolver);
    }
}