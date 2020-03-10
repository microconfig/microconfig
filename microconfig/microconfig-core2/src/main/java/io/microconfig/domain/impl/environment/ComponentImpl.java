package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ResolvedProperties;
import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeSupplier;
import io.microconfig.domain.impl.properties.PropertiesProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

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
    public List<ResolvedProperties> resolvePropertiesForEachConfigType() {
        return providerByConfigType.values().stream()
                .map(this::buildProperties)
                .collect(toList());
    }

    @Override
    public ResolvedProperties resolvePropertiesForConfigType(ConfigTypeSupplier configTypeSupplier) {
        ConfigType configType = configTypeSupplier.chooseType(providerByConfigType.keySet());
        return buildProperties(usingProviderFor(configType));
    }

    private ResolvedProperties buildProperties(PropertiesProvider propertiesProvider) {
        return propertiesProvider.buildProperties(name, type, env);
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