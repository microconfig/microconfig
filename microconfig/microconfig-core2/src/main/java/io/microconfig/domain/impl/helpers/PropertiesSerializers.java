package io.microconfig.domain.impl.helpers;

import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;

import java.io.File;
import java.util.Collection;

import static io.microconfig.domain.impl.properties.PropertyImpl.containsYamlProperties;
import static io.microconfig.service.ioservice.ConfigFormat.PROPERTIES;
import static io.microconfig.service.ioservice.ConfigFormat.YAML;
import static io.microconfig.service.ioservice.factory.ConfigIoServiceFactory.configIoService;
import static io.microconfig.utils.FileUtils.delete;

public class PropertiesSerializers {
    public static PropertySerializer<File> toFileIn(File dir) {
        return (componentName, configType, properties) -> {
            String extension = extensionByContent(properties);
            File file = new File(dir, componentName + "/" + configType.getResultFileName() + extension);
            delete(file);

            if (!properties.isEmpty()) {
                configIoService().writeTo(file).write(properties);
            }
            return file;
        };
    }

    public static PropertySerializer<String> asString() {
        return (componentName, configType, properties) ->
                configIoService().writeTo(new File("", extensionByContent(properties))).serialize(properties);
    }

    private static String extensionByContent(Collection<Property> properties) {
        return properties.isEmpty() || containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }
}