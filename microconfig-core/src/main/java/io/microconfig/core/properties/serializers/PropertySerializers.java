package io.microconfig.core.properties.serializers;

import io.microconfig.core.configtypes.ConfigTypeImpl;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySerializer;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.info;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class PropertySerializers {
    public static BiConsumer<File, Collection<Property>> withConfigDiff() {
        ConfigDiff configDiff = new ConfigDiff();
        return configDiff::storeDiffFor;
    }

    public static PropertySerializer<File> toFileIn(File dir) {
        return toFileIn(dir, (_1, _2) -> {
        });
    }

    public static PropertySerializer<ConfigResult> asConfigResult() {
        return (properties, configType, componentName, __) -> {
            String fileName = configType.getResultFileName() + extensionByConfigFormat(properties).extension();
            String output = properties.isEmpty() ? "" : configIo().writeTo(new File(fileName)).serialize(properties);
            return new ConfigResult(fileName, configType.getName(), output);
        };
    }

    public static PropertySerializer<File> toFileIn(File dir, BiConsumer<File, Collection<Property>> listener) {
        return (properties, configType, componentName, __) -> {
            Function<ConfigFormat, File> getResultFile = cf -> new File(dir, componentName + "/" + configType.getResultFileName() + cf.extension());

            File resultFile = getResultFile.apply(extensionByConfigFormat(properties));
            listener.accept(resultFile, properties);
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

    public static PropertySerializer<File> withLegacySupportSaveTo(PropertySerializer<File> serializer,
                                                                   EnvironmentRepository environmentRepository) {
        return (properties, configType, componentName, environment) -> {
            if (configType.getName().equals(APPLICATION.getName())) {
                Object envSource = environmentRepository.getByName(environment).getSource();
                if (envSource != null && envSource.toString().endsWith(".json")) {
                    configType = new ConfigTypeImpl(configType.getName(), configType.getSourceExtensions(), "service");
                }
            }

            return serializer.serialize(properties, configType, componentName, environment);
        };
    }
}