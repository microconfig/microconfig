package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySerializer;

import java.io.File;
import java.util.Collection;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.impl.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.info;

public class PropertySerializers {
    public static PropertySerializer<File> toFileIn(File dir) {
        return (properties, configType, componentName, __) -> {
            String extension = extensionByConfigFormat(properties);
            File resultFile = new File(dir, componentName + "/" + configType.getResultFileName() + extension);
            delete(resultFile);

            if (!properties.isEmpty()) {
                configIo().writeTo(resultFile).write(properties);
                info("Generated " + componentName + "/" + resultFile.getName());
            }
            return resultFile;
        };
    }

    public static PropertySerializer<String> asString() {
        return (properties, _2, _3, _4) -> configIo()
                .writeTo(new File(extensionByConfigFormat(properties)))
                .serialize(properties);
    }

    private static String extensionByConfigFormat(Collection<Property> properties) {
        return properties.stream().anyMatch(p -> p.getConfigFormat() == YAML) ?
                YAML.extension() :
                PROPERTIES.extension();
    }
}