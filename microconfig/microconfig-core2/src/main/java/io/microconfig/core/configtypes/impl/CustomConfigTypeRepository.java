package io.microconfig.core.configtypes.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microconfig.core.configtypes.impl.ConfigTypeImpl.byName;
import static io.microconfig.core.configtypes.impl.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@RequiredArgsConstructor
public class CustomConfigTypeRepository implements ConfigTypeRepository {
    private static final String DESCRIPTOR = "microconfig.yaml";

    private final FsReader fsReader;
    private final File descriptorFile;

    public static ConfigTypeRepository findDescriptorIn(File rootDir, FsReader fsReader) {
        return new CustomConfigTypeRepository(fsReader, new File(rootDir, DESCRIPTOR));
    }

    @Override
    public List<ConfigType> getConfigTypes() {
        if (!descriptorFile.exists()) return emptyList();

        List<ConfigType> types = parseConfigTypes();
        if (!types.isEmpty()) {
            announce("Using settings from " + descriptorFile);
        }
        return types;
    }

    private List<ConfigType> parseConfigTypes() {
        try {
            return new MicroconfigDescriptor(fsReader.readFully(descriptorFile)).getConfigTypes();
        } catch (RuntimeException e) {
            throw new RuntimeException("Can't parse descriptor: " + descriptorFile, e);
        }
    }

    @RequiredArgsConstructor
    private static class MicroconfigDescriptor {
        private static final String CONFIG_TYPES = "configTypes";
        private static final String SOURCE_EXTENSIONS = "sourceExtensions";
        private static final String RESULT_FILE_NAME = "resultFileName";

        private final String content;

        @SuppressWarnings("unchecked")
        private List<ConfigType> getConfigTypes() {
            Map<String, Object> types = new Yaml().load(content);
            List<Object> configTypes = (List<Object>) types.getOrDefault(CONFIG_TYPES, emptyList());
            return forEach(configTypes, this::parse);
        }

        @SuppressWarnings("unchecked")
        private ConfigType parse(Object configTypeObject) {
            if (configTypeObject instanceof String) {
                return byName(configTypeObject.toString());
            }

            Map<String, Object> configType = (Map<String, Object>) configTypeObject;
            String type = configType.keySet().iterator().next();
            Map<String, Object> attributes = (Map<String, Object>) configType.get(type);
            Set<String> sourceExtensions = attributes.containsKey(SOURCE_EXTENSIONS) ? new LinkedHashSet<>((List<String>) attributes.get(SOURCE_EXTENSIONS)) : singleton(type);
            String resultFileName = (String) attributes.getOrDefault(RESULT_FILE_NAME, type);

            return byNameAndExtensions(type, sourceExtensions, resultFileName);
        }
    }
}