package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;
import io.microconfig.utils.reader.Io;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microconfig.factory.configtype.ConfigTypeImpl.byName;
import static io.microconfig.factory.configtype.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.Logger.announce;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class MicroconfigDescriptorProvider implements ConfigTypeProvider {
    private static final String MICROCONFIG_SETTINGS = "microconfig.yaml";
    private final Path rootDir;
    private final Io io;

    @Override
    public List<ConfigType> getTypes() {
        File file = microconfigDescriptor();
        if (!file.exists()) return emptyList();

        List<ConfigType> configs = parse(file);
        if (!configs.isEmpty()) {
            announce("Using settings from " + MICROCONFIG_SETTINGS);
        }
        return configs;
    }

    private File microconfigDescriptor() {
        return new File(rootDir.toFile(), MICROCONFIG_SETTINGS);
    }

    @SuppressWarnings("unchecked")
    private List<ConfigType> parse(File file) {
        try {
            Map<String, Object> types = new Yaml().load(io.readFully(file));
            List<Object> configTypes = (List<Object>) types.getOrDefault("configTypes", emptyList());
            return configTypes.stream()
                    .map(this::parseType)
                    .collect(toList());
        } catch (RuntimeException e) {
            throw new RuntimeException("Can't parse Microconfig descriptor '" + file + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ConfigType parseType(Object configTypeObject) {
        if (configTypeObject instanceof String) {
            return byName(configTypeObject.toString());
        }

        Map<String, Object> configType = (Map<String, Object>) configTypeObject;
        String type = configType.keySet().iterator().next();
        Map<String, Object> attributes = (Map<String, Object>) configType.get(type);
        Set<String> sourceExtensions = attributes.containsKey("sourceExtensions") ? new LinkedHashSet<>((List<String>) attributes.get("sourceExtensions")) : singleton(type);
        String resultFileName = (String) attributes.getOrDefault("resultFileName", type);

        return byNameAndExtensions(type, sourceExtensions, resultFileName);
    }
}