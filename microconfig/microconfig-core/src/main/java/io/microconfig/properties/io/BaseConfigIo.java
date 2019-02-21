package io.microconfig.properties.io;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class BaseConfigIo implements ConfigIo {
    private final ConfigIo yamlFormat = new YamlConfigIo();
    private final ConfigIo propertiesFormat = new PropertiesConfigIo();

    public static ConfigIo getInstance() {
        return new BaseConfigIo();
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

    private ConfigIo select(File file) {
        return file.getName().endsWith(".yaml") ? yamlFormat : propertiesFormat;
    }
}