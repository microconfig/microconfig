package io.microconfig.configs.resolver;

import io.microconfig.configs.Property;

public interface PropertyResolver {
    String resolve(Property property, RootComponent root);
}
