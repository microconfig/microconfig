package io.microconfig.core.resolvers.placeholder.strategies.environment;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.environments.impl.repository.EnvironmentException;
import io.microconfig.core.properties.Property;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.PlaceholderSource;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.core.properties.impl.PropertyImpl.tempProperty;
import static io.microconfig.core.resolvers.placeholder.strategies.PlaceholderSource.ENV_SOURCE;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class EnvironmentResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentRepository environmentRepository;
    private final Map<String, EnvProperty> propertyByKey;

    @Override
    public Optional<Property> resolve(String component, String key, String env, String configType) {
        EnvProperty envProperty = propertyByKey.get(key);
        if (envProperty == null) return empty();

        Environment environment = getEnvironment(env);
        if (environment == null) return empty();

        return envProperty.resolveFor(component, environment)
                .map(value -> tempProperty(key, value, env, new PlaceholderSource(component, ENV_SOURCE)));
    }

    private Environment getEnvironment(String environment) {
        try {
            return environmentRepository.getByName(environment);
        } catch (EnvironmentException e) {
            return null;
        }
    }
}