package io.microconfig.properties.io;

import io.microconfig.properties.Property;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

public class YamlConfigIo implements ConfigIo {
    @Override
    public Map<String, String> read(File file) {
        return !file.exists() ? emptyMap() : YamlUtils.readAsFlatMap(file);
    }

    @Override
    public void append(File file, Map<String, String> properties) {
        YamlUtils.write(file, properties, APPEND);
    }

    @Override
    public void write(File file, Collection<Property> properties) {
        write(file, properties.stream()
                .filter(p -> !p.isTemp())
                .collect(toMap(Property::getKey, Property::getValue))
        );
    }

    @Override
    public void write(File file, Map<String, String> properties) {
        YamlUtils.write(file, properties);
    }
}