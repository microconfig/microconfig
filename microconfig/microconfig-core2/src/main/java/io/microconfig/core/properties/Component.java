package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigTypeFilter;

public interface Component {
    String getName();

    String getType();

    String getEnvironment();

    CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter);
}