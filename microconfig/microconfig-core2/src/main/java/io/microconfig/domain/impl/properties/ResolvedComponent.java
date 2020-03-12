package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.RequiredArgsConstructor;

import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component original;
    private final Resolver resolver;

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
        return property -> property.resolveBy(resolver);
    }
}