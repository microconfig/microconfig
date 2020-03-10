package io.microconfig.domain.impl.helpers;

import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

import static io.microconfig.domain.impl.properties.PropertyImpl.containsYamlProperties;
import static io.microconfig.service.ioservice.ConfigFormat.PROPERTIES;
import static io.microconfig.service.ioservice.ConfigFormat.YAML;
import static io.microconfig.service.ioservice.factory.ConfigIoServiceFactory.configIoService;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.error;

public class PropertySerializers {
    public static PropertySerializer<File> toFileIn(File dir) {
        return toFileIn(dir, __ -> {
        });
    }

    public static PropertySerializer<File> toFileIn(File dir, Consumer<File> listener) {
        return (componentName, configType, properties) -> {
            String extension = extensionByContent(properties);
            File resultFile = new File(dir, componentName + "/" + configType.getResultFileName() + extension);
            listener.accept(resultFile);
            delete(resultFile);

            if (!properties.isEmpty()) {
                configIoService().writeTo(resultFile).write(properties);
            }
            listener.accept(resultFile);
            return resultFile;
        };
    }

    public static PropertySerializer<String> asString() {
        return (componentName, configType, properties) ->
                configIoService().writeTo(new File("", extensionByContent(properties))).serialize(properties);
    }

    private static String extensionByContent(Collection<Property> properties) {
        return properties.isEmpty() || containsYamlProperties(properties) ? YAML.extension() : PROPERTIES.extension();
    }

    public static Consumer<File> calculatePropertyDiff() {
        return new PropertiesDiffSerializer();
    }
}