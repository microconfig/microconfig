package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.ResolverStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.microconfig.properties.Property.Source.systemSource;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


@RequiredArgsConstructor
public class SpecialResolverStrategy implements ResolverStrategy {
    private final EnvironmentProvider environmentProvider;
    private final Map<String, SpecialKey> specialKeys;

    public SpecialResolverStrategy(EnvironmentProvider environmentProvider, List<SpecialKey> keys) {
        this(environmentProvider, keys.stream().collect(toMap(SpecialKey::key, identity())));
    }

    @Override
    public Optional<Property> resolve(String key, Component component, String envName) {
        SpecialKey specialKey = specialKeys.get(key);
        if (specialKey == null) return empty();

        Environment environment = getEnvironment(envName);
        if (environment == null) return empty();

        return specialKey.value(component, environment)
                .map(value -> new Property(key, value, envName, systemSource(), true));
    }

    private Environment getEnvironment(String environment) {
        try {
            return environmentProvider.getByName(environment);
        } catch (EnvironmentNotExistException e) {
            return null;
        }
    }

    public interface SpecialKey {
        String key();

        Optional<String> value(Component component, Environment environment);
    }
}