package io.microconfig.properties.serializer;

import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK;
import static org.yaml.snakeyaml.DumperOptions.LineBreak.WIN;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN;

@RequiredArgsConstructor
public class PropertiesSerializerImpl implements PropertySerializer {
    private final File componentsDir;
    private final String fileName;
    private final OutputFormat outputFormat;

    public PropertiesSerializerImpl(File componentsDir, String fileName) {
        this(componentsDir, fileName, OutputFormat.byFileExtension(fileName));
    }

    @Override
    public Optional<File> serialize(String component, Collection<Property> properties) {
        File file = pathFor(component);
        delete(file);
        if (containsOnlySystemProperties(properties)) return empty();

        write(file, outputFormat.serialize(properties));
        return of(file);
    }

    @Override
    public File pathFor(String component) {
        return new File(componentsDir, component + "/" + fileName);
    }

    private boolean containsOnlySystemProperties(Collection<Property> properties) {
        return properties.stream().allMatch(p -> p.getSource().isSystem());
    }

    public enum OutputFormat {
        PROPERTIES {
            @Override
            String serialize(Collection<Property> properties) {
                return properties.stream()
                        .filter(p -> !p.isTemp())
                        .map(Property::toString)
                        .collect(joining(LINE_SEPARATOR));
            }
        },
        YAML {
            @Override
            String serialize(Collection<Property> properties) {
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(BLOCK);
                dumperOptions.setDefaultScalarStyle(PLAIN);
                dumperOptions.setLineBreak(WIN);
                dumperOptions.setIndent(2);
                dumperOptions.setPrettyFlow(true);

                Yaml yaml = new Yaml(dumperOptions);
                return yaml.dump(toTree(properties));
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
        };

        public static OutputFormat byFileExtension(String fileName) {
            return Stream.of(values())
                    .filter(format -> fileName.endsWith("." + format.name().toLowerCase()))
                    .findFirst()
                    .orElse(PROPERTIES);
        }

        abstract String serialize(Collection<Property> properties);
    }
}