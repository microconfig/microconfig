package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.List;

public interface PropertiesRepository {
    List<Property> getProperties(String componentType, String environment, ConfigType configType);
}