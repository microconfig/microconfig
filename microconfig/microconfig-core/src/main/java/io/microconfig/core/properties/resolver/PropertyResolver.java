package io.microconfig.core.properties.resolver;

import io.microconfig.core.properties.Property;

public interface PropertyResolver {
    String resolve(Property property, EnvComponent root);
}
