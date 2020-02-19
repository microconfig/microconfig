package io.microconfig.core.properties.serializer.file;

import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;

import static io.microconfig.core.properties.Property.containsYamlProperties;
import static io.microconfig.core.properties.io.ioservice.selector.FileFormat.PROPERTIES;
import static io.microconfig.core.properties.io.ioservice.selector.FileFormat.YAML;

@RequiredArgsConstructor
public class FilenameGeneratorImpl implements FilenameGenerator {
    private final File destinationComponentDir;
    private final String serviceInnerDir;
    private final String resultFilename;

    @Override
    public File fileFor(String component, String env, Collection<Property> properties) {
        return new File(destinationComponentDir, dir(component) + "/" + name() + extension(properties));
    }

    private String dir(String component) {
        return serviceInnerDir == null ? component : component + "/" + serviceInnerDir;
    }

    private String name() {
        return resultFilename;
    }

    private String extension(Collection<Property> properties) {
        String outputFormat = System.getProperty("outputFormat");
        if (outputFormat != null) return outputFormat;

        return containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }
}