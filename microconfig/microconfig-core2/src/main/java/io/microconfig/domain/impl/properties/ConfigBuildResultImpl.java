package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigBuildResult;
import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.function.UnaryOperator;

import static io.microconfig.io.StreamUtils.map;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
public class ConfigBuildResultImpl implements ConfigBuildResult {
    private final String component;
    private final String environment;
    private final ConfigType configType;
    @Getter
    @With(PRIVATE)
    private final List<Property> properties;

    @Override
    public String getConfigType() {
        return configType.getType();
    }

    @Override
    public ConfigBuildResult forEachProperty(UnaryOperator<Property> operator) {
        return withProperties(map(properties, operator));
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(properties, configType, component, environment);
    }
}