package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypesFilter;
import io.microconfig.core.properties.CompositeProperties;

public interface Component {
    String getName();

    String getType();

    String getEnvironment();

    CompositeProperties getPropertiesFor(ConfigTypesFilter filter);
}