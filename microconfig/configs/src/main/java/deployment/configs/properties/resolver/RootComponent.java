package deployment.configs.properties.resolver;

import deployment.configs.environment.Component;
import lombok.EqualsAndHashCode;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode
public class RootComponent {
    private final Component rootComponent;
    private final String rootComponentEnv;

    public RootComponent(Component rootComponent, String rootComponentEnv) {
        this.rootComponent = requireNonNull(rootComponent);
        this.rootComponentEnv = requireNonNull(rootComponentEnv);
    }

    public Component getRootComponent() {
        return rootComponent;
    }

    public String getRootComponentEnv() {
        return rootComponentEnv;
    }

    @Override
    public String toString() {
        return rootComponent.getName() + "[" + rootComponentEnv + "]";
    }
}
