package io.microconfig.domain;

import java.util.Collection;

public interface PropertySerializer<T> {
    T serialize(String componentName, ConfigType configType, Collection<Property> properties);
}