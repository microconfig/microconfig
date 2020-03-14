package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.domain.impl.properties.CompositeConfigBuildResult.composite;
import static io.microconfig.io.StreamUtils.map;

@RequiredArgsConstructor
public class FileBasedComponent implements Component {
    private final ConfigTypes types;
    private final FileSystemGraph fsGraph;

    @Getter
    private final String name;
    @Getter
    private final String environment;

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        List<ConfigType> filteredTypes = filter.selectTypes(types.getTypes());
        return composite(
                map(filteredTypes, type -> new ConfigBuildResultImpl(name, environment, type, readPropertiesWith(type)))
        );
    }

    private List<Property> readPropertiesWith(ConfigType type) {
        return null;
    }
}