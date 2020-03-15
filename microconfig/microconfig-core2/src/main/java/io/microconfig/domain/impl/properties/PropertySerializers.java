package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;

import java.io.File;
import java.util.List;

import static io.microconfig.domain.impl.properties.PropertyImpl.containsYamlProperties;
import static io.microconfig.io.FileUtils.delete;
import static io.microconfig.io.formats.ConfigFormat.PROPERTIES;
import static io.microconfig.io.formats.ConfigFormat.YAML;
import static io.microconfig.io.formats.factory.ConfigIoServiceFactory.configIo;

public class PropertySerializers {
    public static PropertySerializer<File> toFileIn(File dir) {
        return (properties, configType, componentName, __) -> {
            String extension = extensionByContent(properties);
            File resultFile = new File(dir, componentName + "/" + configType.getResultFileName() + extension);
            delete(resultFile);

            if (!properties.isEmpty()) {
                configIo().writeTo(resultFile).write(properties);
            }
            return resultFile;
        };
    }

    public static PropertySerializer<String> asString() {
        return (properties, _2, _3, _4) -> configIo()
                .writeTo(new File(extensionByContent(properties)))
                .serialize(properties);
    }

    private static String extensionByContent(List<Property> properties) {
        return properties.isEmpty() || containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }
}