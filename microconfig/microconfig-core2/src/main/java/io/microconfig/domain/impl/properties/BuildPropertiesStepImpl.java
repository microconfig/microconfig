package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Map;

import static io.microconfig.utils.StreamUtils.map;

@RequiredArgsConstructor
public class BuildPropertiesStepImpl implements BuildPropertiesStep {
    private final Map<ConfigType, PropertiesProvider> providerByConfigType;

    private final String componentName;
    private final String componentType;
    private final String env;

    @Override
    public ResultComponents forEachConfigType() {
        return new ResultComponentsImpl(
                map(providerByConfigType.values(), this::buildProperties)
        );
    }

    @Override
    public ResultComponents forConfigType(ConfigTypeFilter configTypeFilter) {
        Collection<ConfigType> filteredTypes = configTypeFilter.filter(providerByConfigType.keySet());
        return new ResultComponentsImpl(
                map(filteredTypes, type -> buildProperties(usingProviderFor(type)))
        );
    }

    private ResultComponent buildProperties(PropertiesProvider propertiesProvider) {
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