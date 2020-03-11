package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.ConfigBuildResultImpl;
import io.microconfig.service.tree.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.domain.impl.properties.ConfigBuildResultsImpl.composite;
import static io.microconfig.utils.StreamUtils.map;

@RequiredArgsConstructor
public class FileBasedComponent implements Component {
    private final ConfigTypes types;
    private final ComponentTree componentTree;

    private final String componentName;
    private final String env;

    @Override
    public String getName() {
        return componentName;
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(types.getTypes());
        return composite(map(filteredTypes, this::readConfigsWithType));
    }

    private ConfigBuildResult readConfigsWithType(ConfigType type) {
        return new ConfigBuildResultImpl(componentName, type, readPropertiesWith(type));
    }

    private List<Property> readPropertiesWith(ConfigType type) {
        return null;
    }
}