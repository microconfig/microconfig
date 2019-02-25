package io.microconfig.configs.resolver;

import io.microconfig.environments.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public class RootComponent {
    private final Component rootComponent;
    private final String rootEnv;

    public RootComponent(Component rootComponent, String rootEnv) {
        this.rootComponent = requireNonNull(rootComponent);
        this.rootEnv = requireNonNull(rootEnv);
    }

    @Override
    public String toString() {
        return rootComponent.getName() + "[" + rootEnv + "]";
    }
}
