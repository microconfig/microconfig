package io.microconfig.core.resolvers.placeholder.strategies.environment.properties;

import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvProperty;

import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

public class EnvironmentProperties {
    public Map<String, EnvProperty> get() {
        return of(
                new EnvNameProperty(),
                new IpProperty(),
                new GroupNameProperty(),
                new ComponentOrderProperty(),
                new PortOffsetProperty()
        ).collect(toMap(EnvProperty::key, identity()));
    }
}