package io.microconfig.domain.impl.environment;

import io.microconfig.domain.*;
import io.microconfig.domain.impl.properties.PropertiesProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class BuildPropertiesStepImpl implements BuildPropertiesStep {
    private final Map<ConfigType, PropertiesProvider> providerByConfigType;

    private final String componentName;
    private final String componentType;
    private final String env;

    @Override
    public ResultComponents forEachConfigType() {
        return forConfigType(t -> t);
    }

    @Override
    public ResultComponents forConfigType(ConfigTypeFilter configTypeFilter) {
        List<ResultComponent> components = configTypeFilter.filter(providerByConfigType.keySet())
                .stream()
                .map(this::providerForType)
                .map(this::buildProperties)
                .collect(toList());
        return new ResultComponentsImpl(components);
    }

    private ResultComponent buildProperties(PropertiesProvider propertiesProvider) {
        return propertiesProvider.buildProperties(componentName, componentType, env);
    }

    private PropertiesProvider providerForType(ConfigType configType) {
        PropertiesProvider provider = providerByConfigType.get(configType);
        if (provider == null) {
            throw new IllegalArgumentException("Config type '" + configType + "' is not configured." +
                    " Supported types: " + providerByConfigType.keySet());
        }
        return provider;
    }
}
