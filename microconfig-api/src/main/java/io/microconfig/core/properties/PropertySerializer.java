package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.Collection;

public interface PropertySerializer<T> {
    T serialize(Collection<Property> properties,
                ConfigType configType,
                String componentName,
                String environment);
}