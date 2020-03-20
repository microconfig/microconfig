package io.microconfig.core.environments.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertiesFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@EqualsAndHashCode(of = {"name", "originalName", "environment"})
@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertiesFactory propertiesFactory;

    @Getter
    private final String name;
    private final String originalName;
    @Getter
    private final String environment;

    @Override
    public Properties getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(configTypeRepository.getConfigTypes());
        return propertiesFactory.getPropertiesOf(name, originalName, environment, filteredTypes);
    }

    @Override
    public String toString() {
        return name;
    }
}