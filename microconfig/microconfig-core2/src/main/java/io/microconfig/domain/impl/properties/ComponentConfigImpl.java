package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.Optional;

import static io.microconfig.io.StreamUtils.forEach;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
public class ComponentConfigImpl implements ComponentConfig {
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
    public ComponentConfig resolveBy(StatementResolver resolver) {
        return withProperties(forEach(properties, p -> p.resolveBy(resolver)));
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return properties.stream()
                .filter(p -> p.getKey().equals(key))
                .findFirst();
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return serializer.serialize(properties, configType, component, environment);
    }
}