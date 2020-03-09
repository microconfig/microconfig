package io.microconfig.core.domain;

import io.microconfig.core.domain.impl.ConfigType;

import java.util.Collection;

public interface PropertiesSerializer<T> {
    T serialize(String componentName, ConfigType configType, Collection<Property> properties);
}