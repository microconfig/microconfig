package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigBuildResult;
import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.UnaryOperator;

import static io.microconfig.utils.StreamUtils.map;

@RequiredArgsConstructor
public class ConfigBuildResultImpl implements ConfigBuildResult {
    private final String componentName;
    private final ConfigType configType;
    @Getter
    private final List<Property> properties;

    @Override
    public String getConfigType() {
        return configType.getType();
    }

    @Override
    public ConfigBuildResult applyForEachProperty(UnaryOperator<Property> operator) {
        return new ConfigBuildResultImpl(componentName, configType, map(properties, operator));
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(componentName, configType, properties);
    }
}