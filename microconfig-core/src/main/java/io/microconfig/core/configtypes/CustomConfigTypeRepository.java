package io.microconfig.core.configtypes;

import io.microconfig.core.properties.repository.ComponentGraph;
import io.microconfig.core.properties.repository.ComponentGraphImpl;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static io.microconfig.core.configtypes.ConfigTypeImpl.byName;
import static io.microconfig.core.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.core.properties.repository.ComponentGraphImpl.traverseFrom;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class CustomConfigTypeRepository implements ConfigTypeRepository {
    private static final String DESCRIPTOR = "microconfig.yaml";

    private final FsReader fsReader;
    private final File descriptorFile;
    private final File rootDir;

    public static ConfigTypeRepository findDescriptorIn(File rootDir, FsReader fsReader) {
        return new CustomConfigTypeRepository(fsReader, new File(rootDir, DESCRIPTOR), rootDir);
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
            return new MicroconfigDescriptor(fsReader.readFully(descriptorFile), fsReader).getConfigTypes(rootDir);
        } catch (RuntimeException e) {
            throw new RuntimeException("Can't parse descriptor: " + descriptorFile, e);
        }
    }

    @RequiredArgsConstructor
    private static class MicroconfigDescriptor {
        private static final String INCLUDE = "include";
        private static final String DEFAULT = "default";

        private final String content;
        private final FsReader reader;

        private List<ConfigType> getConfigTypes(File rootDir) {
            List<ConfigType> configTypes = new MicroconfigTypeDescriptor(content).getConfigTypes();
            configTypes.addAll(getIncludedConfigTypes(rootDir));
            configTypes.addAll(getDefaultConfigTypes());
            return configTypes;
        }

        private List<ConfigType> getDefaultConfigTypes() {
            boolean includeDefault = getIncludes().stream().anyMatch(name -> name.equals(DEFAULT));
            return includeDefault ? new StandardConfigTypeRepository().getConfigTypes() : emptyList();
        }

        private List<ConfigType> getIncludedConfigTypes(File rootDir) {
            if (!(new File(rootDir, "components").exists())) return emptyList();
            ComponentGraph componentGraph = traverseFrom(rootDir);
            return getIncludes().stream()
                    .filter(name -> !name.equals(DEFAULT))
                    .map(componentGraph::getFolderOf)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(file -> new File(file, DESCRIPTOR))
                    .map(reader::readFully)
                    .map(MicroconfigTypeDescriptor::new)
                    .map(MicroconfigTypeDescriptor::getConfigTypes)
                    .flatMap(Collection::stream)
                    .collect(toList());
        }

        @SuppressWarnings("unchecked")
        private List<String> getIncludes() {
            Map<String, Object> descriptor = new Yaml().load(content);
            return (List<String>) descriptor.getOrDefault(INCLUDE, emptyList());
        }
    }

    @RequiredArgsConstructor
    private static class MicroconfigTypeDescriptor {
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