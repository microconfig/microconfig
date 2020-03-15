package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.domain.impl.properties.CompositeComponentPropertiesImpl.resultsOf;
import static io.microconfig.utils.StreamUtils.forEach;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypeRepository configTypeRepository;
    private final PropertyRepository propertyRepository;

    @Getter
    private final String name;
    @Getter
    private final String type;
    @Getter
    private final String environment;

    @Override
    public CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(configTypeRepository.getConfigTypes());
        return resultsOf(forEach(filteredTypes, this::readConfigs));
    }

    private ComponentProperties readConfigs(ConfigType configType) {
        List<Property> properties = propertyRepository.getProperties(type, environment, configType);
        return new ComponentPropertiesImpl(name, environment, configType, properties);
    }
}