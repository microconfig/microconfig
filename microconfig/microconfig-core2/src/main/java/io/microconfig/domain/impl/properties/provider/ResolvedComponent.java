package io.microconfig.domain.impl.properties.provider;

import io.microconfig.domain.Component;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.ConfigTypeFilter;
import io.microconfig.domain.Property;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResolvedComponent implements Component {
    private final Component component;

    @Override
    public String getName() {
        return component.getName();
    }

    @Override
    public ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter) {
        return component.buildPropertiesFor(filter)
                .forEachProperty(this::resolve);
    }

    private Property resolve(Property property) {
        return null;
    }
}