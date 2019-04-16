package io.microconfig.factory.configtypes;

import io.microconfig.factory.ConfigType;
import io.microconfig.factory.ConfigsTypeProvider;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microconfig.factory.configtypes.ConfigTypeImpl.byName;
import static io.microconfig.factory.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.announce;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ConfigTypeFileProvider implements ConfigsTypeProvider {
    private static final String MICROCONFIG_SETTINGS = "microconfig.yaml";

    @Override
    public List<ConfigType> getConfigTypes(File rootDir) {
        File file = configFile(rootDir);
        if (!file.exists()) return emptyList();

        List<ConfigType> configs = parse(file);
        if (!configs.isEmpty()) {
            announce("Using settings from " + MICROCONFIG_SETTINGS);
        }
        return configs;

    }

    private File configFile(File rootDir) {
        return new File(rootDir, MICROCONFIG_SETTINGS);
    }

    @SuppressWarnings("unchecked")
    private List<ConfigType> parse(File file) {
        try {
            Map<String, Object> types = new Yaml().load(readFully(file));
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