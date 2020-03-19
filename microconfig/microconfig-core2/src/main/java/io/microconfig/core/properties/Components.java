package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigTypeFilter;

import java.util.List;

public interface Components {
    List<Component> asList();

    CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter);
}