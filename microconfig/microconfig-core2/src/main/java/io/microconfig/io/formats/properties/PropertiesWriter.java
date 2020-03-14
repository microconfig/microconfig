package io.microconfig.io.formats.properties;

import io.microconfig.domain.Property;
import io.microconfig.io.formats.ConfigWriter;
import io.microconfig.io.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;

import static io.microconfig.io.FileUtils.LINES_SEPARATOR;
import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor
class PropertiesWriter implements ConfigWriter {
    private final File file;

    @Override
    public void write(Map<String, String> properties) {
        doWrite(serializeMap(properties));
    }

    @Override
    public void write(Collection<Property> properties) {
        doWrite(serialize(properties));
    }

    @Override
    public String serialize(Collection<Property> properties) {
        return properties.stream()
                .filter(p -> !p.isTemp())
                .map(Property::toString)
                .collect(joining(LINES_SEPARATOR));
    }

    private String serializeMap(Map<String, String> properties) {
        return properties.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining(LINES_SEPARATOR));
    }

    private void doWrite(String lines, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), lines, openOptions);
    }
}