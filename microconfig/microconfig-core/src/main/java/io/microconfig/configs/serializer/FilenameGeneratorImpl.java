package io.microconfig.configs.serializer;

import io.microconfig.commands.buildconfig.factory.ConfigType;
import io.microconfig.configs.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;

import static io.microconfig.configs.Property.containsYamlProperties;
import static io.microconfig.configs.io.ioservice.selector.FileFormat.PROPERTIES;
import static io.microconfig.configs.io.ioservice.selector.FileFormat.YAML;

@RequiredArgsConstructor
public class FilenameGeneratorImpl implements FilenameGenerator {
    private final File destinationComponentDir;
    private final String serviceInnerDir;
    private final ConfigType configType;

    @Override
    public File fileFor(String component, String env, Collection<Property> properties) {
        return new File(destinationComponentDir, dir(component) + "/" + name() + extension(properties));
    }

    private String dir(String component) {
        return serviceInnerDir == null ? component : component + "/" + serviceInnerDir;
    }

    private String name() {
        return configType.getResultFileName();
    }

    private String extension(Collection<Property> properties) {
        String outputFormat = System.getProperty("outputFormat");
        if (outputFormat != null) return outputFormat;
        return containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }
}