package io.microconfig.commands.factory;

import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.commands.factory.PropertyType.SECRET;
import static io.microconfig.properties.Property.withoutTempValues;
import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.FileUtils.userHome;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.PropertiesUtils.append;
import static io.microconfig.utils.PropertiesUtils.loadPropertiesAsMap;

@RequiredArgsConstructor
public class SecretPropertiesPostProcessor implements PropertiesPostProcessor {
    private final File secretFile;

    public SecretPropertiesPostProcessor() {
        this(new File(userHome(), "/secret/secret.properties"));
    }

    @Override
    public void process(File serviceDir, String serviceName, Map<String, Property> properties) {
        Map<String, String> props = withoutTempValues(properties);
        if (props.isEmpty()) return;

        doMerge(serviceName, new LinkedHashMap<>(props));
        delete(new File(serviceDir, SECRET.getResultFile()));
    }

    private synchronized void doMerge(String serviceName, Map<String, String> properties) {
        loadPropertiesAsMap(secretFile).keySet().forEach(properties::remove);

        if (!properties.isEmpty()) {
            announce("Appending new values to secret.properties: " + serviceName + " -> " + properties);
            append(secretFile, properties);
        }
    }
}