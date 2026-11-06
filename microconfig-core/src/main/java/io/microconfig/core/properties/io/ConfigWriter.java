package io.microconfig.core.properties.io;

import io.microconfig.core.properties.Property;

import java.util.Collection;
import java.util.Map;

public interface ConfigWriter {
    void write(Map<String, String> properties);

    void write(Collection<Property> properties);

    String serialize(Collection<Property> properties);

    // the value as it's written to the file, to compare it with the value read back from the file
    default String serializedValue(Property property) {
        return property.getValue();
    }
}
