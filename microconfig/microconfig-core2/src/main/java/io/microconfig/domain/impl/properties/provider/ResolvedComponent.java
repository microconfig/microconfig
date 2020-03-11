package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.ConfigBuildResultImpl;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Map;

import static io.microconfig.domain.impl.properties.ConfigBuildResultsImpl.composite;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component component;

    @Override
    public String getName() {
        return component.getName();
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        return composite(component.buildPropertiesFor(filter).save(this::resolve));
    }

    private ConfigBuildResult resolve(String componentName, ConfigType configType, Collection<Property> properties) {
        return new ConfigBuildResultImpl(componentName, configType, resolve(properties, configType.getType()));
    }

    private Map<String, Property> resolve(Collection<Property> properties, String type) {
        return null;
    }
}
