package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.PropertiesSerializer;
import io.microconfig.core.domain.Property;

import java.io.File;
import java.util.Collection;

import static io.microconfig.core.domain.impl.PropertyImpl.containsYamlProperties;
import static io.microconfig.core.service.io.ioservice.factory.ConfigIoServiceFactory.configIoService;
import static io.microconfig.core.service.io.ioservice.selector.FileFormat.PROPERTIES;
import static io.microconfig.core.service.io.ioservice.selector.FileFormat.YAML;
import static io.microconfig.utils.FileUtils.delete;

public class PropertiesSerializers {
    public static PropertiesSerializer<File> toFileIn(File resultComponentDir) {
        return (componentName, configType, properties) -> {
            String extension = getFormat(properties);
            File file = new File(resultComponentDir, componentName + "/" + configType.getResultFileName() + extension);
            delete(file);

            if (!properties.isEmpty()) {
                configIoService().writeTo(file).write(properties);
            }
            return file;
        };
    }

    public static PropertiesSerializer<String> asString() {
        return (componentName, configType, properties) ->
                configIoService().writeTo(new File("", getFormat(properties))).serialize(properties);
    }

    private static String getFormat(Collection<Property> properties) {
        return properties.isEmpty() || containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }
}