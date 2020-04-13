package io.microconfig.core.properties.resolvers.placeholder.strategies.standard;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.core.configtypes.ConfigTypeFilters.configTypeWithName;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;

    @Override
    public Optional<Property> resolve(String root, String component, String key, String environment, String configType) {
        return environmentRepository.getOrCreateByName(environment)
                .getOrCreateComponentWithName(component)
                .getPropertiesFor(configTypeWithName(configType))
                .getPropertyWithKey(key);
    }
}