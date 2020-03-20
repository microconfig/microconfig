package io.microconfig.core.properties.resolver;

import io.microconfig.core.environments.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public class EnvComponent {
    private final Component component;
    private final String environment;

    public EnvComponent(Component component, String environment) {
        this.component = requireNonNull(component);
        this.environment = requireNonNull(environment);
    }

    @Override
    public String toString() {
        return component.getName() + "[" + environment + "]";
    }
}
