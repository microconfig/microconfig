package io.microconfig.io;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class BaseConfigFormat implements ConfigFormat {
    private final ConfigFormat yamlFormat = new YamlConfigFormat();
    private final ConfigFormat propertiesFormat = new PropertiesConfigFormat();

    public static ConfigFormat getInstance() {
        return new BaseConfigFormat();
    }

    @Override
    public Map<String, String> read(File file) {
        return select(file).read(file);
    }

    @Override
    public void append(File file, Map<String, String> properties) {
        select(file).append(file, properties);
    }

    @Override
    public void write(File file, Collection<Property> properties) {
        select(file).write(file, properties);
    }

    @Override
    public void write(File file, Map<String, String> properties) {
        select(file).write(file, properties);
    }

    private ConfigFormat select(File file) {
        return file.getName().endsWith(".yaml") ? yamlFormat : propertiesFormat;
    }
}