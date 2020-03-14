package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import io.microconfig.service.tree.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.domain.impl.properties.CompositeConfigBuildResult.composite;
import static io.microconfig.utils.StreamUtils.map;

@RequiredArgsConstructor
public class FileBasedComponent implements Component {
    private final ConfigTypes types;
    private final ComponentTree componentTree;

    private final String component;
    private final String environment;

    @Override
    public String getName() {
        return component;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(types.getTypes());
        return composite(
                map(filteredTypes, type -> new ConfigBuildResultImpl(component, environment, type, readPropertiesWith(type)))
        );
    }

    private List<Property> readPropertiesWith(ConfigType type) {
        return null;
    }
}