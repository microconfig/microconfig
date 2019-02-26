package io.microconfig.configs.serializer;

import io.microconfig.commands.factory.ConfigType;
import io.microconfig.configs.files.provider.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static io.microconfig.configs.files.provider.ConfigFileFilters.extensionFilter;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class FilenameGeneratorImpl implements FilenameGenerator {
    private static final String YAML = ".yaml";
    private static final String PROPERTIES = ".properties";

    private final File destinationComponentDir;
    private final String serviceInnerDir;
    private final ConfigType configType;
    private final ComponentTree componentTree;

    @Override
    public File fileFor(String component) {
        return new File(destinationComponentDir, serviceDir(component) + "/" + fileName(component));
    }

    private String serviceDir(String component) {
        return serviceInnerDir == null ? component : serviceInnerDir + "/" + component;
    }

    private String fileName(String component) {
        return configType.getResultFileName() + outputFormatFor(component);
    }

    private String outputFormatFor(String component) {
        String outputFormat = System.getProperty("outputFormat");
        if (outputFormat != null) return outputFormat;
        if (containsYamlSources(component)) return ".yaml";
        return PROPERTIES;
    }

    private boolean containsYamlSources(String component) {
        List<File> sourceFiles = componentTree.getConfigFiles(component, extensionFilter(configType.getConfigExtensions())).collect(toList());
        if (sourceFiles.stream().anyMatch(file -> file.getName().endsWith(YAML))) return true;

        return false;
    }
}
