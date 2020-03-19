package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.List;

public interface PropertiesFactory {
    Properties getComponentProperties(String componentType,
                                      String environment,
                                      List<ConfigType> configTypes);
}