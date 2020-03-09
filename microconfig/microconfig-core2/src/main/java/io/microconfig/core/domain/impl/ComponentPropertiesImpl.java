package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.ComponentProperties;
import io.microconfig.core.domain.PropertiesSerializer;
import io.microconfig.core.domain.Property;
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
        return configType.getName();
    }

    @Override
    public <T> T serialize(PropertiesSerializer<T> serializer) {
        return serializer.serialize(componentName, configType, propertyByKey.values());
    }
}