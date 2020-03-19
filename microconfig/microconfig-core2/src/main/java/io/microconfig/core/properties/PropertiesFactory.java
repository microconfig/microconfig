package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

import java.util.List;

public interface PropertiesFactory {
    CompositeProperties getPropertiesOf(String component,
                                        String environment,
                                        List<ConfigType> configTypes);
}