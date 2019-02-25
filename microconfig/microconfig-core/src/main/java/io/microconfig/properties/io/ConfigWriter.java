package io.microconfig.properties.io;

import io.microconfig.properties.Property;

import java.util.Collection;
import java.util.Map;

public interface ConfigWriter {
    void write(Map<String, String> properties);

    void write(Collection<Property> properties);

    void append(Map<String, String> properties);
}
