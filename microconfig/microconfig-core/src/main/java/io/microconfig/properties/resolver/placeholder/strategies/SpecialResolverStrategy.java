package io.microconfig.properties.resolver.placeholder.strategies;

import io.microconfig.environments.*;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.placeholder.ResolverStrategy;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;

import static io.microconfig.properties.Property.Source.systemSource;
import static io.microconfig.utils.FileUtils.userHomeString;
import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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

    public Set<String> keys() {
        return specialKeys.keySet();
    }

    public interface SpecialKey {
//        String key();

        Optional<String> value(Component component, Environment environment);
    }
}