package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;

import java.util.List;

public interface PropertiesRepository {
    List<Property> getProperties(String componentType, String environment, ConfigType configType);
}