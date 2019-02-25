package io.microconfig.configs.files.io.yaml;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class YamlConfigWriter implements ConfigWriter {
    private final File file;

    @Override
    public void write(Map<String, String> properties) {
        doWrite(properties);
    }

    @Override
    public void write(Collection<Property> properties) {
        doWrite(properties.stream()
                .filter(p -> !p.isTemp())
                .collect(toMap(Property::getKey, Property::getValue))
        );
    }

    @Override
    public void append(Map<String, String> properties) {
        doWrite(properties, APPEND); //todo can break yaml format
    }

    private void doWrite(Map<String, String> flatProperties, OpenOption... openOptions) {
        Map<String, Object> tree = toTree(flatProperties);
        String yaml = toYaml(tree);
        FileUtils.write(file.toPath(), yaml, openOptions);
    }

    private Map<String, Object> toTree(Map<String, String> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.forEach((k, v) -> propertyToTree(k, v, result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void propertyToTree(String key, String value, Map<String, Object> result) {
        String[] parts = key.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object oldValue = result.get(part);
            if (oldValue == null) {
                Map<String, Object> newMap = new TreeMap<>();
                result.put(part, newMap);
                result = newMap;
            } else if (oldValue instanceof Map) {
                result = (Map<String, Object>) oldValue;
            } else {
                parts[i + 1] = part + "." + parts[i + 1];
            }
        }

        result.put(parts[parts.length - 1], value);
    }

    private String toYaml(Map<String, Object> tree) {
        StringBuilder result = new StringBuilder();
        return dump(result, tree, 0, true);
    }

    private String dump(StringBuilder result, Map<String, Object> tree, int indent, boolean emptyLine) {
        for (Map.Entry<String, Object> entry : tree.entrySet()) {
            for (int i = 0; i < indent; i++) {
                result.append(' ');
            }
            result.append(entry.getKey());
            dumpValue(result, entry.getValue(), indent + 2);
            if (emptyLine) {
                result.append(LINES_SEPARATOR);
            }

        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private void dumpValue(StringBuilder result, Object value, int indent) {
        if (value instanceof Map) {
            result.append(':')
                    .append(LINES_SEPARATOR);
            dump(result, (Map) value, indent, false);
        } else {
            result.append(": ")
                    .append(value)
                    .append(LINES_SEPARATOR);
        }
    }
}