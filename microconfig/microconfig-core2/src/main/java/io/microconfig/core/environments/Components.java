package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.Properties;

import java.util.List;

public interface Components {
    List<Component> asList();

    Properties getPropertiesFor(ConfigTypeFilter filter);
}