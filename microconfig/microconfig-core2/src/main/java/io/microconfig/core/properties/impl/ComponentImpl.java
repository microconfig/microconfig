package io.microconfig.core.properties.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.properties.Component;
import io.microconfig.core.properties.ComponentProperties;
import io.microconfig.core.properties.CompositeComponentProperties;
import io.microconfig.core.properties.Property;
import io.microconfig.utils.StreamUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.properties.impl.CompositeComponentPropertiesImpl.resultsOf;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertiesRepository propertiesRepository;

    @Getter
    private final String name;
    @Getter
    private final String type;
    @Getter
    private final String environment;

    @Override
    public CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(configTypeRepository.getConfigTypes());
        return resultsOf(StreamUtils.forEach(filteredTypes, this::readConfigs));
    }

    private ComponentProperties readConfigs(ConfigType configType) {
        List<Property> properties = propertiesRepository.getProperties(type, environment, configType);
        return new ComponentPropertiesImpl(name, environment, configType, properties);
    }

    @Override
    public String toString() {
        return name;
    }
}