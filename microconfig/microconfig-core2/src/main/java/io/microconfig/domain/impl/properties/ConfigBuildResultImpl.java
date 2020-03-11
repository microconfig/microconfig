package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigBuildResult;
import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public class ConfigBuildResultImpl implements ConfigBuildResult {
    private final String componentName;
    private final ConfigType configType;
    @Getter
    private final Map<String, Property> propertyByKey;

    @Override
    public String getConfigType() {
        return configType.getType();
    }

    @Override
    public ConfigBuildResult forEachProperty(UnaryOperator<Property> operator) {
        return null;
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(componentName, configType, propertyByKey.values());
    }
}