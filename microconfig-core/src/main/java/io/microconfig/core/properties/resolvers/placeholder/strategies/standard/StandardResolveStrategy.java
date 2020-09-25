package io.microconfig.core.properties.resolvers.placeholder.strategies.standard;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.Placeholder;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.core.configtypes.ConfigTypeFilters.configTypeWithName;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        return environmentRepository.getOrCreateByName(placeholder.getEnvironment())
                .findComponentWithName(placeholder.getComponent())
                .getPropertiesFor(configTypeWithName(placeholder.getConfigType()))
                .getPropertyWithKey(placeholder.getKey());
    }
}