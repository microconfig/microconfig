package io.microconfig.core.properties.serializers;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeImpl;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySerializer;
import io.microconfig.core.templates.Template;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.utils.FileUtils.*;
import static io.microconfig.utils.Logger.info;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class PropertySerializers {
    public static BiConsumer<File, Collection<Property>> withConfigDiff() {
        ConfigDiff configDiff = new ConfigDiff();
        return configDiff::storeDiffFor;
    }

    public static PropertySerializer<ConfigResult> asConfigResult() {
        return (properties, templates, configType, componentName, __) -> {
            String fileName = configType.getResultFileName() + extensionByConfigFormat(properties).extension();
            String output = properties.isEmpty() ? "" : configIo().writeTo(new File(fileName)).serialize(properties);
            return new ConfigResult(componentName, configType.getName(), fileName, output, templates);
        };
    }

    public static PropertySerializer<File> toFileIn(File dir, BiConsumer<File, Collection<Property>> listener) {
        return (properties, templates, configType, componentName, __) -> {
            Function<ConfigFormat, File> getResultFile = cf -> new File(dir, componentName + "/" + configType.getResultFileName() + getFileExtension(configType, cf));

            ConfigFormat cf = extensionByConfigFormat(properties);
            File resultFile = getResultFile.apply(cf);
            listener.accept(resultFile, properties);
            if (properties.isEmpty()) {
                delete(resultFile);
                delete(getResultFile.apply(PROPERTIES));
            } else {
                configIo(cf).writeTo(resultFile).write(properties);
                info("Generated " + componentName + "/" + resultFile.getName());
                templates.forEach(t -> copyTemplate(t, componentName));
            }
            return resultFile;
        };
    }

    private static String getFileExtension(ConfigType configType, ConfigFormat configFormat) {
        String resultFileExtension = configType.getResultFileExtension();
        return resultFileExtension != null ? resultFileExtension : configFormat.extension();
    }

    private static void copyTemplate(Template template, String componentName) {
        write(template.getDestination(), template.getContent());
        copyPermissions(template.getSource().toPath(), template.getDestination().toPath());
        info("Copied '" + componentName + "' template ../"
                + template.getSource().getParentFile().getName()
                + "/" + template.getSource().getName() + " -> " + template.getFileName());
    }

    public static PropertySerializer<String> asString() {
        return (properties, _2, _3, _4, _5) -> configIo()
                .writeTo(new File(extensionByConfigFormat(properties).extension()))
                .serialize(properties);
    }

    private static ConfigFormat extensionByConfigFormat(Collection<Property> properties) {
        return properties.isEmpty() || properties.stream().anyMatch(p -> p.getConfigFormat() == YAML) ? YAML : PROPERTIES;
    }

    public static PropertySerializer<File> withLegacySupport(PropertySerializer<File> serializer,
                                                             EnvironmentRepository environmentRepository) {
        return (properties, templates, configType, componentName, environment) -> {
            if (configType.getName().equals(APPLICATION.getName())) {
                File envSource = environmentRepository.getByName(environment).getSource();
                if (envSource != null && envSource.toString().endsWith(".json")) {
                    configType = new ConfigTypeImpl(configType.getName(), configType.getSourceExtensions(), "service", null);
                }
            }

            return serializer.serialize(properties, templates, configType, componentName, environment);
        };
    }
}