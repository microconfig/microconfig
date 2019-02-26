package io.microconfig.configs.serializer;

import io.microconfig.commands.factory.ConfigType;
import io.microconfig.configs.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;

import static io.microconfig.configs.files.format.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.format.FileFormat.YAML;

@RequiredArgsConstructor
public class FilenameGeneratorImpl implements FilenameGenerator {
    private final File destinationComponentDir;
    private final String serviceInnerDir;
    private final ConfigType configType;

    @Override
    public File fileFor(String component, Collection<Property> properties) {
        return new File(destinationComponentDir, serviceDir(component) + "/" + fileName() + extension(properties));
    }

    private String serviceDir(String component) {
        return serviceInnerDir == null ? component : serviceInnerDir + "/" + component;
    }

    private String fileName() {
        return configType.getResultFileName();
    }

    private String extension(Collection<Property> properties) {
        String outputFormat = System.getProperty("outputFormat");
        if (outputFormat != null) return outputFormat;

        return properties.stream()
                .anyMatch(p -> p.getSource().isYaml()) ? YAML.extension() : PROPERTIES.extension();
    }
}