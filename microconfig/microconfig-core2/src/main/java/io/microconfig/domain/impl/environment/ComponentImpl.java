package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentResolver;
import io.microconfig.domain.ConfigType;
import io.microconfig.domain.impl.properties.PropertiesProvider;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ComponentImpl implements Component {
    private final Map<ConfigType, PropertiesProvider> providerByConfigType;

    private final String name;
    private final String type;
    private final String env;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ComponentResolver buildProperties() {
        return new ComponentResolverImpl(providerByConfigType, name, type, env);
    }
}