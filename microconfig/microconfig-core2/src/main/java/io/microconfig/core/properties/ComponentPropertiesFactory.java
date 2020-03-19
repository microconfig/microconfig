package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.List;

public interface ComponentPropertiesFactory {
    CompositeComponentProperties getComponentProperties(String componentType, String environment, List<ConfigType> filteredTypes);
}
