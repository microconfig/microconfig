package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.CompositeComponentProperties;

import java.util.List;

public interface Components {
    List<Component> asList();

    CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter);
}