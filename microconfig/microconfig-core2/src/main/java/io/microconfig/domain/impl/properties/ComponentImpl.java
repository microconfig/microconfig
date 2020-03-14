package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.domain.impl.properties.CompositeComponentConfigurationImpl.resultsOf;
import static io.microconfig.io.StreamUtils.forEach;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypes configTypes;
    private final PropertyRepository propertyRepository;

    @Getter
    private final String alias;
    @Getter
    private final String type;
    @Getter
    private final String environment;

    @Override
    public CompositeComponentConfiguration getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(configTypes.getTypes());
        return resultsOf(forEach(filteredTypes, this::readConfigs));
    }

    private ComponentConfiguration readConfigs(ConfigType configType) {
        List<Property> properties = propertyRepository.getProperties(alias, type, environment, configType);
        return new ComponentConfigurationImpl(alias, environment, configType, properties);
    }
}