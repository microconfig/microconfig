package io.microconfig.io;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface ConfigIoService {
    Map<String, String> read(File file);

    void write(File file, Collection<Property> properties);

    void append(File file, Map<String, String> properties);

    void write(File file, Map<String, String> properties);
}