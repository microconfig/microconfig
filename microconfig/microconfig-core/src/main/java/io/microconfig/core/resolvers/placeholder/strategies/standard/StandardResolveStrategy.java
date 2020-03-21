package io.microconfig.core.resolvers.placeholder.strategies.standard;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.configTypeWithName;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;

    @Override
    public Optional<Property> resolve(String component, String key, String environment, String configType) {
        Property property = environmentRepository.getOrCreateByName(environment)
                .findComponentWithName(component, false)
                .getPropertiesFor(configTypeWithName(configType))
                .asList().get(0)
                .getProperties()
                .get(key);
        return ofNullable(property);
    }
}