package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.CompositeComponentProperties;

public interface Component {
    String getName();

    String getType();

    String getEnvironment();

    CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter);
}