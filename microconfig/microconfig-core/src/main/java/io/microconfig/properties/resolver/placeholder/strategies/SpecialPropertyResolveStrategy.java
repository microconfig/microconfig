package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.ResolveStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.properties.Property.systemSourceProperty;
import static java.util.Optional.empty;


@RequiredArgsConstructor
public class SpecialPropertyResolveStrategy implements ResolveStrategy {
    private final EnvironmentProvider environmentProvider;
    private final Map<String, SpecialProperty> specialKeys;

    @Override
    public Optional<Property> resolve(String key, Component component, String envName) {
        SpecialProperty specialProperty = specialKeys.get(key);
        if (specialProperty == null) return empty();

        Environment environment = getEnvironment(envName);
        if (environment == null) return empty();

        return specialProperty.value(component, environment)
                .map(value -> systemSourceProperty(key, value, envName));
    }

    private Environment getEnvironment(String environment) {
        try {
            return environmentProvider.getByName(environment);
        } catch (EnvironmentNotExistException e) {
            return null;
        }
    }

    public interface SpecialProperty {
        String key();

        Optional<String> value(Component component, Environment environment);
    }
}