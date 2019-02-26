package io.microconfig.configs.resolver.placeholder.strategies;

import io.microconfig.configs.Property;
import io.microconfig.configs.PropertySource;
import io.microconfig.configs.resolver.placeholder.ResolveStrategy;
import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static io.microconfig.configs.Property.tempProperty;
import static io.microconfig.configs.PropertySource.specialSource;
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
                .map(value -> tempProperty(key, value, envName, specialSource(component, "specials")));
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