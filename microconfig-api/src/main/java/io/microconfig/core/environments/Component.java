package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.Properties;

public interface Component {
    String getName();

    String getOriginalName();

    String getEnvironment();

    Properties getPropertiesFor(ConfigTypeFilter filter);
}