package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.CompositeProperties;

public interface Component {
    String getName();

    String getType();

    String getEnvironment();

    CompositeProperties getPropertiesFor(ConfigTypeFilter filter);
}