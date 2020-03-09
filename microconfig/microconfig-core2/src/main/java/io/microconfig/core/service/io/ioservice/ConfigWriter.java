package io.microconfig.core.service.io.ioservice;


import io.microconfig.core.domain.Property;

import java.util.Collection;
import java.util.Map;

public interface ConfigWriter {
    void write(Map<String, String> properties);

    void write(Collection<Property> properties);

    String serialize(Collection<Property> properties);
}
