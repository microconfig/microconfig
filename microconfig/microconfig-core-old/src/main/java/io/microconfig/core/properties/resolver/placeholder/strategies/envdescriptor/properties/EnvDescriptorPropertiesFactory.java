package io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.properties;

import io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.EnvProperty;

import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

public class EnvDescriptorPropertiesFactory {
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