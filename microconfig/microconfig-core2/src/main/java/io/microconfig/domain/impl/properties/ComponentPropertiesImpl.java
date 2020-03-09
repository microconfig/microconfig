package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ComponentProperties;
import io.microconfig.domain.PropertiesSerializer;
import io.microconfig.domain.Property;
import io.microconfig.domain.ConfigType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ComponentPropertiesImpl implements ComponentProperties {
    private final String componentName;
    private final ConfigType configType;
    @Getter
    private final Map<String, Property> propertyByKey;

    @Override
    public String getConfigType() {
        return configType.getType();
    }

    @Override
    public <T> T serialize(PropertiesSerializer<T> serializer) {
        return serializer.serialize(componentName, configType, propertyByKey.values());
    }
}