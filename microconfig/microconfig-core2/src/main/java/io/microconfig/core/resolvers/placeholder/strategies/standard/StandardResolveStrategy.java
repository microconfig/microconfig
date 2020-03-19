package io.microconfig.core.resolvers.placeholder.strategies.standard;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.Placeholder;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.configTypeWithName;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;

    @Override
    public Optional<Property> resolve(Placeholder p) {
        return environmentRepository.getOrCreateByName(p.getEnvironment())
                .findComponentWithName(p.getComponent(), false)
                .getPropertiesFor(configTypeWithName(p.getConfigType()))
                .getPropertyWithKey(p.getValue());
    }
}