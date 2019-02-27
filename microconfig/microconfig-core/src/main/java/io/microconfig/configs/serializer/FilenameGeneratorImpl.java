package io.microconfig.configs.serializer;

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
    private final String resultFileName;

    @Override
    public File fileFor(String component, Collection<Property> properties) {
        return new File(destinationComponentDir, dir(component) + "/" + name() + extension(properties));
    }

    private String dir(String component) {
        return serviceInnerDir == null ? component : serviceInnerDir + "/" + component;
    }

    private String name() {
        return resultFileName;
    }

    private String extension(Collection<Property> properties) {
        String outputFormat = System.getProperty("outputFormat");
        if (outputFormat != null) return outputFormat;

        return properties
                .stream()
                .anyMatch(p -> p.getSource().isYaml()) ?
                YAML.extension() : PROPERTIES.extension();
    }
}