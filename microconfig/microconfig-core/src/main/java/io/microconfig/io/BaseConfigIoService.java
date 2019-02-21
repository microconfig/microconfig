package io.microconfig.io;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class BaseConfigIoService implements ConfigIoService {
    private static final ConfigIoService propertiesFormat = new PropertiesIoService();
    private static final ConfigIoService yamlFormat = new YamlIoServiceImpl();

    public static ConfigIoService getInstance() {
        return new BaseConfigIoService();
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

    private ConfigIoService select(File file) {
        return file.getName().endsWith(".yaml") ? yamlFormat : propertiesFormat;
    }
}