package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.standard;

import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.configTypeWithName;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        return environmentRepository.getOrCreateByName(placeholder.getEnvironment())
                .findComponentWithName(placeholder.getComponent(), false)
                .getPropertiesFor(configTypeWithName(placeholder.getConfigType()))
                .getPropertyWithKey(placeholder.getValue());
    }
}
