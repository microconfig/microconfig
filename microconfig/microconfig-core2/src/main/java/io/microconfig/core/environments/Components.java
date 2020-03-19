package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypesFilter;
import io.microconfig.core.properties.CompositeProperties;

import java.util.List;

public interface Components {
    List<Component> asList();

    CompositeProperties getPropertiesFor(ConfigTypesFilter filter);
}