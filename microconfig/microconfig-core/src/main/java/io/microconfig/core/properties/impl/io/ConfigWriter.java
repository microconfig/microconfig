package io.microconfig.core.properties.impl.io;


import io.microconfig.core.properties.Property;

import java.util.Collection;
import java.util.Map;

public interface ConfigWriter {
    void write(Map<String, String> properties);

    void write(Collection<Property> properties);

    String serialize(Collection<Property> properties);
}
