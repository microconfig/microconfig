package io.microconfig.core.properties.resolvers.placeholder.strategies.environment;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.environments.repository.EnvironmentException;
import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.Placeholder;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.PropertyImpl.property;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class EnvironmentResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;
    private final Map<String, EnvProperty> propertyByKey;

    @Override
    public Optional<Property> resolve(Placeholder placeholder) {
        EnvProperty envProperty = propertyByKey.get(placeholder.getKey());
        if (envProperty == null) return empty();

        Environment environment = getEnvironment(placeholder.getEnvironment());
        if (environment == null) return empty();

        return envProperty.resolveFor(placeholder.getComponent(), environment)
                .map(value -> property(placeholder.getKey(), value, PROPERTIES,
                        new DeclaringComponentImpl(placeholder.getConfigType(), placeholder.getComponent(), placeholder.getEnvironment())));
    }

    private Environment getEnvironment(String environment) {
        try {
            return environmentRepository.getByName(environment);
        } catch (EnvironmentException e) {
            return null;
        }
    }
}