package io.microconfig.core.properties.resolvers.placeholder.strategies.component;

import java.util.Optional;

public interface ComponentProperty {
    String key();

    Optional<String> resolveFor(String component, String environment);
}