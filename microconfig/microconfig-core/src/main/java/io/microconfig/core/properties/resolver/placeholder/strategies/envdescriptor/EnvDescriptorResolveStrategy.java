package io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentDoesNotExistException;
import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.core.properties.Property.tempProperty;
import static io.microconfig.core.properties.sources.SpecialSource.envSource;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class EnvDescriptorResolveStrategy implements PlaceholderResolveStrategy {
    private final EnvironmentProvider environmentProvider;
    private final Map<String, EnvProperty> properties;

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String envName) {
        EnvProperty specialProperty = properties.get(propertyKey);
        if (specialProperty == null) return empty();

        Environment environment = getEnvironment(envName);
        if (environment == null) return empty();

        return specialProperty.value(component, environment)
                .map(value -> tempProperty(propertyKey, value, envName, envSource(component)));
    }

    private Environment getEnvironment(String environment) {
        try {
            return environmentProvider.getByName(environment);
        } catch (EnvironmentDoesNotExistException e) {
            return null;
        }
    }
}