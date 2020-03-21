package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySerializer;

import java.io.File;
import java.util.Collection;

import static io.microconfig.core.properties.impl.io.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.impl.io.ConfigFormat.YAML;
import static io.microconfig.core.properties.impl.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.info;

public class PropertySerializers {
    public static PropertySerializer<File> toFileIn(File dir) {
        return (properties, configType, componentName, __) -> {
            String extension = extensionByContent(properties);
            File resultFile = new File(dir, componentName + "/" + configType.getResultFileName() + extension);
            delete(resultFile);

            if (!properties.isEmpty()) {
                configIo().writeTo(resultFile).write(properties);
                info("Generated " + resultFile.getName() + " for " + componentName);
            }
            return resultFile;
        };
    }

    public static PropertySerializer<String> asString() {
        return (properties, _2, _3, _4) -> configIo()
                .writeTo(new File(extensionByContent(properties)))
                .serialize(properties);
    }

    private static String extensionByContent(Collection<Property> properties) {
        return properties.isEmpty() || containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }

    private static boolean containsYamlProperties(Collection<Property> properties) {
        return properties.stream()
                .map(Property::getSource)
                .filter(s -> s instanceof FilePropertySource)
                .map(FilePropertySource.class::cast)
                .anyMatch(FilePropertySource::isYaml);
    }
}