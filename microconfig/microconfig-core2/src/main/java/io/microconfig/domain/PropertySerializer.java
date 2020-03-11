package io.microconfig.domain;

import java.util.List;

public interface PropertySerializer<T> {
    T serialize(List<Property> properties, ConfigType configType, String componentName, String environment);
}