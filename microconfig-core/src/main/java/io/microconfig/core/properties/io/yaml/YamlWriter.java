package io.microconfig.core.properties.io.yaml;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;

import static io.microconfig.core.properties.io.properties.PropertiesText.isPropertiesText;
import static io.microconfig.core.properties.io.properties.PropertiesText.unescape;
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
                        .filter(p -> !p.isVar())
                        .collect(toSortedMap(Property::getKey, this::serializedValue))
        );
    }

    // a component with both .properties and YAML files is written as YAML, so its .properties values are converted to plain text
    @Override
    public String serializedValue(Property property) {
        return isPropertiesText(property.getDeclaringComponent()) ? unescape(property.getValue()) : property.getValue();
    }

    private String serializeMap(Map<String, String> properties) {
        return yamlTree.toYaml(properties);
    }

    private void doWrite(String yaml, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), yaml, openOptions);
    }
}