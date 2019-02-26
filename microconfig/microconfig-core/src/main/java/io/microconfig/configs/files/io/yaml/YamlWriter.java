package io.microconfig.configs.files.io.yaml;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.*;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.Logger.align;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class YamlWriter implements ConfigWriter {
    private static final int OFFSET = 2;
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
        doWrite(properties, APPEND);//todo can break yaml format
    }

    private void doWrite(Map<String, String> flatProperties, OpenOption... openOptions) {
        ArrayList<String> strings = new ArrayList<>(flatProperties.keySet());
        for (String string : strings) {
            if (!string.startsWith("cr.cf.tfs.out")) {
                flatProperties.remove(string);
            }
        }

        Map<String, Object> tree = toTree(flatProperties);
        FileUtils.write(file.toPath(), toYaml(tree), openOptions);
    }

    Map<String, Object> toTree(Map<String, String> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.forEach((k, v) -> propertyToTree(k, v, result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void propertyToTree(String key, String value, Map<String, Object> result) {
        String[] parts = splitKey(key);
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

        result.put(parts[parts.length - 1], offsetForMultilineValue(parts.length, value));
    }

    private String[] splitKey(String key) {
        return key.split("\\.(?![^\\[]*])"); // split on '.' that are not inside of []
    }

    private String offsetForMultilineValue(int parts, String value) {
        if (!value.startsWith("-")) return value;

        return (LINES_SEPARATOR + value)
                .replace(LINES_SEPARATOR, align(LINES_SEPARATOR, (parts + 1) * OFFSET));
    }

    private String toYaml(Map<String, Object> tree) {
        StringBuilder result = new StringBuilder();
        dump(result, tree, 0, true);
        return result.toString();
    }

    private void dump(StringBuilder result, Map<String, Object> tree, int indent, boolean emptyLine) {
        tree.forEach((key, value) -> {
            result.append(align("", indent)).append(key);
            dumpValue(result, value, indent + OFFSET);

            if (emptyLine) {
                result.append(LINES_SEPARATOR);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void dumpValue(StringBuilder result, Object value, int indent) {
        if (value instanceof Map) {
            result.append(':').append(LINES_SEPARATOR);
            dump(result, (Map) value, indent, false);
            return;
        }

        result.append(": ")
                .append(value)
                .append(LINES_SEPARATOR);
    }
}