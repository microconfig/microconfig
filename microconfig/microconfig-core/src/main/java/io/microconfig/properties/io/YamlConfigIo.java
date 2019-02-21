package io.microconfig.properties.io;

import io.microconfig.properties.Property;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

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

    }

    @Override
    public void write(File file, Collection<Property> properties) {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(BLOCK);
        dumperOptions.setDefaultScalarStyle(PLAIN);
        dumperOptions.setLineBreak(WIN);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);

        Yaml yaml = new Yaml(dumperOptions);
        String dump = yaml.dump(toTree(properties));


    }

    private Map<String, Object> toTree(Collection<Property> properties) {
        Map<String, Object> result = new TreeMap<>();
        properties.stream()
                .filter(p -> !p.isTemp())
                .filter(p -> !p.getSource().isSystem())
                .forEach(p -> add(p, result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void add(Property p, Map<String, Object> result) {
        String[] split = p.getKey().split("\\.");
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

        result.put(split[split.length - 1], p.typedValue());
    }

    @Override
    public void write(File file, Map<String, String> properties) {

    }


}