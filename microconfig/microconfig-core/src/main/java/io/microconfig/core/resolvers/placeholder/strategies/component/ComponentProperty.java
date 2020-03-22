package io.microconfig.core.resolvers.placeholder.strategies.component;

import java.util.Optional;

public interface ComponentProperty {
    String key();

    Optional<String> resolveFor(String component, String environment);
}