package io.microconfig.properties.io.ioservice;

import io.microconfig.properties.Property;

import java.util.Collection;
import java.util.Map;

public interface ConfigWriter {
    void write(Map<String, String> properties);

    void write(Collection<Property> properties);

    String serialize(Collection<Property> properties);

    void append(Map<String, String> properties);
}
