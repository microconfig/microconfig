package io.microconfig.features.secrets;

import io.microconfig.configs.Property;
import io.microconfig.configs.files.io.ConfigIoService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static io.microconfig.configs.Property.withoutTempValues;
import static java.util.Collections.emptySet;

@RequiredArgsConstructor
public class SecretServiceImpl implements SecretService {
    private final File secretFile;
    private final ConfigIoService configIoService;

    @Override
    public Set<String> updateSecrets(Map<String, Property> componentProperties) {
        Map<String, String> props = withoutTempValues(componentProperties);
        if (props.isEmpty()) return emptySet();

        return update(new LinkedHashMap<>(props));
    }

    private synchronized Set<String> update(Map<String, String> properties) {
        configIoService.read(secretFile)
                .propertiesAsMap()
                .keySet()
                .forEach(properties::remove);

        if (!properties.isEmpty()) {
            configIoService.writeTo(secretFile).append(properties);
        }

        return properties.keySet();
    }
}