package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies;

import io.microconfig.domain.Environments;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.resolvers.placeholder.Placeholder;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.configTypeWithName;

@RequiredArgsConstructor
public class StandardResolveStrategy implements PlaceholderResolveStrategy {
    private final Environments environments;

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        return environments.getOrCreateWithName(placeholder.getEnvironment())
                .findComponentWithName(placeholder.getComponent(), false)
                .getPropertiesFor(configTypeWithName(placeholder.getConfigType()))
                .getPropertyWithKey(placeholder.getValue());
    }
}
