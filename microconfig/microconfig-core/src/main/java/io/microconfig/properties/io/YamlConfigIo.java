package io.microconfig.properties.io;

import io.microconfig.properties.Property;
import io.microconfig.utils.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.properties.Property.typeValue;
import static io.microconfig.utils.FileUtils.LINE_SEPARATOR;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK;
import static org.yaml.snakeyaml.DumperOptions.LineBreak.WIN;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN;

public class YamlConfigIo implements ConfigIo {
    @Override
    public Map<String, String> read(File file) {
        return null;
    }

    @Override
    public void append(File file, Map<String, String> properties) {
        FileUtils.write(file.toPath(), LINE_SEPARATOR, APPEND);
        doWrite(file, toTree(properties), APPEND);
    }

    @Override
    public void write(File file, Collection<Property> properties) {
        doWrite(file, toTree(properties));
    }

    @Override
    public void write(File file, Map<String, String> properties) {
        doWrite(file, toTree(properties));
    }

    private Map<String, Object> toTree(Collection<Property> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.stream()
                .filter(p -> !p.isTemp())
                .forEach(p -> add(p.getKey(), p.typedValue(), result));
        return result;
    }

    private Map<String, Object> toTree(Map<String, String> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.forEach((k, v) -> add(k, typeValue(v), result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void add(String key, Object value, Map<String, Object> result) {
        String[] split = key.split("\\.");
        for (int i = 0; i < split.length - 1; i++) {
            String part = split[i];
            result = (Map<String, Object>) result.compute(part, (k, v) -> {
                if (v == null) return new TreeMap<>();
                if (v instanceof Map) return v;
                TreeMap<Object, Object> map = new TreeMap<>();
                map.put(k, v);
                return map;
            });
        }

        result.put(split[split.length - 1], value);
    }

    private void doWrite(File file, Map<String, Object> tree, OpenOption... openOptions) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(BLOCK);
        options.setDefaultScalarStyle(PLAIN);
        options.setLineBreak(WIN);
        options.setIndent(2);
        options.setPrettyFlow(true);

        FileUtils.write(file.toPath(), new Yaml(options).dump(tree), openOptions);
    }
}