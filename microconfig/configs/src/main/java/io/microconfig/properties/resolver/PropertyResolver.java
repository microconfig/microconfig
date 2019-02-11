package io.microconfig.properties.resolver;

import io.microconfig.properties.Property;

public interface PropertyResolver {
    String resolve(Property property, RootComponent root);
}
