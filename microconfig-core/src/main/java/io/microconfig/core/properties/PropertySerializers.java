package io.microconfig.core.properties;

import java.io.File;
import java.util.Collection;
import java.util.function.Function;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.info;

public class PropertySerializers {
    public static PropertySerializer<File> toFileIn(File dir) {
        return (properties, configType, componentName, __) -> {
            Function<ConfigFormat, File> getResultFile = cf ->
                    new File(dir, componentName + "/" + configType.getResultFileName() + cf);

            File resultFile = getResultFile.apply(extensionByConfigFormat(properties));
            if (properties.isEmpty()) {
                delete(resultFile);
                delete(getResultFile.apply(PROPERTIES));
            } else {
                configIo().writeTo(resultFile).write(properties);
                info("Generated " + componentName + "/" + resultFile.getName());
            }
            return resultFile;
        };
    }

    public static PropertySerializer<String> asString() {
        return (properties, _2, _3, _4) -> configIo()
                .writeTo(new File(extensionByConfigFormat(properties).extension()))
                .serialize(properties);
    }

    private static ConfigFormat extensionByConfigFormat(Collection<Property> properties) {
        return properties.isEmpty() || properties.stream().anyMatch(p -> p.getConfigFormat() == YAML) ?
                YAML : PROPERTIES;
    }
}