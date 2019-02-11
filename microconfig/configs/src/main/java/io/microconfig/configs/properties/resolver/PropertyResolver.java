package io.microconfig.configs.properties.resolver;

import io.microconfig.configs.properties.Property;

public interface PropertyResolver {
    String resolve(Property property, RootComponent root);
}
