package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.List;

public interface PropertySerializer<T> {
    T serialize(List<Property> properties, ConfigType configType, String componentName, String environment);
}