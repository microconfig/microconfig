package io.microconfig.commands.postprocessors;

import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.io.ConfigIoService;
import io.microconfig.configs.resolver.RootComponent;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.commands.factory.ConfigType.SECRET;
import static io.microconfig.configs.Property.withoutTempValues;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.FileUtils.userHome;
import static io.microconfig.utils.Logger.announce;

@RequiredArgsConstructor
public class SecretPropertiesPostProcessor implements PropertiesPostProcessor {
    private final File secretFile;
    private final ConfigIoService configIo;

    public SecretPropertiesPostProcessor(ConfigIoService configIo) {
        this(new File(userHome(), "/secret/secret.properties"), configIo);
    }

    @Override
    public void process(RootComponent currentComponent, File destinationDir,
                        Map<String, Property> componentProperties, ConfigProvider ignore) {
        Map<String, String> props = withoutTempValues(componentProperties);
        if (props.isEmpty()) return;

        doMerge(currentComponent.getRootComponent().getName(), new LinkedHashMap<>(props));
        delete(new File(destinationDir, SECRET.getResultFileName()));
    }

    private synchronized void doMerge(String serviceName, Map<String, String> properties) {
        configIo.read(secretFile)
                .asMap()
                .keySet()
                .forEach(properties::remove);

        if (!properties.isEmpty()) {
            announce("Appending new values to secret.properties: " + serviceName + " -> " + properties);
            configIo.writeTo(secretFile).append(properties);
        }
    }
}