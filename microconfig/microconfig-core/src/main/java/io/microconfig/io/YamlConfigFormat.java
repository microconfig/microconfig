package io.microconfig.io;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class YamlConfigFormat implements ConfigFormat {
    @Override
    public Map<String, String> read(File file) {
        return null;
    }

    @Override
    public void append(File file, Map<String, String> properties) {

    }

    @Override
    public void write(File file, Collection<Property> properties) {

    }

    @Override
    public void write(File file, Map<String, String> properties) {

    }
}