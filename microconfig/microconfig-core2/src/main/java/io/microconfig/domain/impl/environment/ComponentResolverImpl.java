package io.microconfig.domain.impl.environment;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.PropertiesProvider;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentResolverImpl implements ComponentResolver {
    private final Map<ConfigType, PropertiesProvider> providerByConfigType;

    private final String componentName;
    private final String componentType;
    private final String env;

    @Override
    public ResolvedComponents forEachConfigType() {
        return new ResolvedComponentsImpl(providerByConfigType.values().stream()
                .map(this::buildProperties)
                .collect(toList()));
    }

    @Override
    public ResolvedComponent forConfigType(ConfigTypeFilter configTypeFilter) {
        ConfigType configType = configTypeFilter.chooseType(providerByConfigType.keySet());
        return buildProperties(usingProviderFor(configType));
    }

    private ResolvedComponent buildProperties(PropertiesProvider propertiesProvider) {
        return propertiesProvider.buildProperties(componentName, componentType, env);
    }

    private PropertiesProvider usingProviderFor(ConfigType configType) {
        PropertiesProvider provider = providerByConfigType.get(configType);
        if (provider == null) {
            throw new IllegalArgumentException("Config type '" + configType + "' is not configured." +
                    " Supported types: " + providerByConfigType.keySet());
        }
        return provider;
    }
}
