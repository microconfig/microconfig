package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.Property;

import java.util.List;

public interface PropertiesRepository {
    List<Property> getPropertiesOf(String componentType, String environment, ConfigType configType);
}