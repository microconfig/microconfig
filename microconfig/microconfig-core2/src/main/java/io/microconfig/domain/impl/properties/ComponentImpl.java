package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

import static io.microconfig.domain.impl.properties.ConfigBuildResultsImpl.resultsOf;
import static io.microconfig.io.StreamUtils.toList;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final ConfigTypes types;
    private final Resolver resolver;
    private final FileSystemGraph fsGraph;

    @Getter
    private final String name;
    @Getter
    private final String environment;

    @Override
    public ConfigBuildResults getPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(types.getTypes());
        return resultsOf(toList(filteredTypes, this::readConfigs));
    }

    private ConfigBuildResult readConfigs(ConfigType type) {
        return new ConfigBuildResultImpl(name, environment, type, resolver, readPropertiesWith(type));
    }

    private List<Property> readPropertiesWith(ConfigType type) {
        return Collections.emptyList();
    }
}