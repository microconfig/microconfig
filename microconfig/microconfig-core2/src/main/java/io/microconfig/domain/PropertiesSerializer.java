package io.microconfig.domain;

import java.util.Collection;

public interface PropertiesSerializer<T> {
    T serialize(String componentName, ConfigType configType, Collection<Property> properties);
}