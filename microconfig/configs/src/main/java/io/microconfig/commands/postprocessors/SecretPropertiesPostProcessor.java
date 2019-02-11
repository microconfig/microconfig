package io.microconfig.commands.postprocessors;

import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static deployment.util.FileUtils.delete;
import static deployment.util.FileUtils.userHome;
import static deployment.util.Logger.announce;
import static deployment.util.PropertiesUtils.append;
import static deployment.util.PropertiesUtils.loadPropertiesAsMap;
import static io.microconfig.commands.factory.PropertyType.SECRET;
import static io.microconfig.properties.Property.withoutTempValues;

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