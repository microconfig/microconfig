package io.microconfig.core.resolvers.placeholder.strategies.standard;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.configTypeWithName;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;

    @Override
    public Optional<Property> resolve(String configType, String component, String environment, String key) {
        return environmentRepository.getOrCreateByName(environment)
                .findComponentWithName(component, false)
                .getPropertiesFor(configTypeWithName(configType))
                .getPropertyWithKey(key);
    }
}