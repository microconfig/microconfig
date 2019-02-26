package io.microconfig.configs.serializer;

import io.microconfig.commands.factory.ConfigType;
import io.microconfig.configs.files.format.FileFormatDetector;
import io.microconfig.configs.files.provider.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.configs.files.format.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.format.FileFormat.YAML;
import static io.microconfig.configs.files.provider.ConfigFileFilters.extensionFilter;

@RequiredArgsConstructor
public class FilenameGeneratorImpl implements FilenameGenerator {
    private final File destinationComponentDir;
    private final String serviceInnerDir;
    private final ConfigType configType;
    private final ComponentTree componentTree;
    private final FileFormatDetector fileFormatDetector;

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

        return containsYamlFiles(component) ? YAML.extension() : PROPERTIES.extension();
    }

    private boolean containsYamlFiles(String component) {
        return componentTree.getConfigFiles(component, extensionFilter(configType.getConfigExtensions()))
                .anyMatch(file -> fileFormatDetector.detectFileFormat(file) == YAML);
    }
}
