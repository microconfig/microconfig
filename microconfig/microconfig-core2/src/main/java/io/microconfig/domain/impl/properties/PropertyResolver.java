package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;

public interface PropertyResolver {
    String resolve(Property property, String component, String environment);
}
