package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.ConfigBuildResultsImpl;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component component;

    @Override
    public String getName() {
        return component.getName();
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        return new ConfigBuildResultsImpl(component.buildPropertiesFor(filter).save(this::resolve));
    }

    private ConfigBuildResult resolve(String componentName, ConfigType configType, Collection<Property> properties) {
        return null;
    }
}
