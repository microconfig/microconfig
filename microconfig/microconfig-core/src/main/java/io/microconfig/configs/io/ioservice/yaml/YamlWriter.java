package io.microconfig.configs.io.ioservice.yaml;

import io.microconfig.configs.Property;
import io.microconfig.configs.io.ioservice.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;

import static io.microconfig.utils.StreamUtils.toSortedMap;
import static java.nio.file.StandardOpenOption.APPEND;

@RequiredArgsConstructor
public class YamlWriter implements ConfigWriter {
    private final YamlTree yamlTree;
    private final File file;

    public YamlWriter(File file) {
        this(new YamlTreeImpl(), file);
    }

    @Override
    public void write(Map<String, String> properties) {
        doWrite(yamlTree.toYaml(properties));
    }

    @Override
    public void write(Collection<Property> properties) {
        doWrite(serialize(properties));
    }

    @Override
    public void append(Map<String, String> properties) {
        doWrite(yamlTree.toYaml(properties), APPEND);//todo can break yaml format
    }

    @Override
    public String serialize(Collection<Property> properties) {
        return yamlTree.toYaml(
                properties.stream()
                        .filter(p -> !p.isTemp())
                        .collect(toSortedMap(Property::getKey, Property::getValue))
        );
    }

    private void doWrite(String yaml, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), yaml, openOptions);
    }
}