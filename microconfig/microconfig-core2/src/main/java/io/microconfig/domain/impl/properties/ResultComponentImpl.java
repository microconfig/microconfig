package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;
import io.microconfig.domain.ResultComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ResultComponentImpl implements ResultComponent {
    @Getter
    private final String componentName;
    private final ConfigType configType;
    @Getter
    private final Map<String, Property> propertyByKey;

    @Override
    public String getConfigType() {
        return configType.getType();
    }

    @Override
    public <T> T serialize(PropertySerializer<T> serializer) {
        return serializer.serialize(componentName, configType, propertyByKey.values());
    }
}