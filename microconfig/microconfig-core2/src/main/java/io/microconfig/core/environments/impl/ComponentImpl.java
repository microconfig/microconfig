package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.ComponentPropertiesFactory;
import io.microconfig.core.properties.CompositeComponentProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypeRepository configTypeRepository;
    private final ComponentPropertiesFactory componentPropertiesFactory;

    @Getter
    private final String name;
    @Getter
    private final String type;
    @Getter
    private final String environment;

    @Override
    public CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(configTypeRepository.getConfigTypes());
        return componentPropertiesFactory.getComponentProperties(type, environment, filteredTypes);
    }

    @Override
    public String toString() {
        return name;
    }
}