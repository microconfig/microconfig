package io.microconfig.domain;

import java.util.List;

public interface PropertySerializer<T> {
    T serialize(String componentName, String environment, ConfigType configType, List<Property> properties);
}