package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.ConfigBuildResultImpl;
import io.microconfig.service.tree.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.domain.impl.properties.ConfigBuildResultsImpl.composite;
import static java.util.stream.Collectors.toList;

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
        return composite(
                filter.selectTypes(types.getTypes())
                        .stream()
                        .map(this::read)
                        .collect(toList())
        );
    }

    private ConfigBuildResult read(ConfigType type) {
        return new ConfigBuildResultImpl(componentName, type, readProperties(type));
    }

    private Map<String, Property> readProperties(ConfigType type) {
        return null;
    }
}