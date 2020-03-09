package io.microconfig.core.service.io.ioservice.yaml;

import io.microconfig.core.domain.Property;
import io.microconfig.core.service.io.ioservice.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;

import static io.microconfig.utils.StreamUtils.toSortedMap;

@RequiredArgsConstructor
public class YamlWriter implements ConfigWriter {
    private final YamlTree yamlTree;
    private final File file;

    public YamlWriter(File file) {
        this(new YamlTreeImpl(), file);
    }

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
        return serializeMap(
                properties.stream()
                        .filter(p -> !p.isTemp())
                        .collect(toSortedMap(Property::getKey, Property::getValue))
        );
    }

    private String serializeMap(Map<String, String> properties) {
        return yamlTree.toYaml(properties);
    }

    private void doWrite(String yaml, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), yaml, openOptions);
    }
}