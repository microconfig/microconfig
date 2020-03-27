package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.Map;

public interface PropertiesRepository {
    Map<String, Property> getPropertiesOf(String originalComponentName,
                                          String environment,
                                          ConfigType configType);
}