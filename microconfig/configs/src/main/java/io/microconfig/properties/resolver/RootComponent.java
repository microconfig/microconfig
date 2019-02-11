package io.microconfig.properties.resolver;

import io.microconfig.environments.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public class RootComponent {
    private final Component rootComponent;
    private final String rootComponentEnv;

    public RootComponent(Component rootComponent, String rootComponentEnv) {
        this.rootComponent = requireNonNull(rootComponent);
        this.rootComponentEnv = requireNonNull(rootComponentEnv);
    }

    @Override
    public String toString() {
        return rootComponent.getName() + "[" + rootComponentEnv + "]";
    }
}
