package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

import static io.microconfig.domain.impl.properties.CompositeComponentConfigurationImpl.resultsOf;
import static io.microconfig.io.StreamUtils.forEach;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypes types;
    private final FileSystemGraph fsGraph;

    @Getter
    private final String name;
    @Getter
    private final String environment;

    @Override
    public CompositeComponentConfiguration getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(types.getTypes());
        return resultsOf(forEach(filteredTypes, this::readConfigs));
    }

    private ComponentConfiguration readConfigs(ConfigType type) {
        return new ComponentConfigurationImpl(name, environment, type, readPropertiesWith(type));
    }

    private List<Property> readPropertiesWith(ConfigType type) {
        return Collections.emptyList();
    }
}