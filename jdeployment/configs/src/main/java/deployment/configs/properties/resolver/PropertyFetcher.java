package deployment.configs.properties.resolver;

import deployment.configs.environment.Component;
import deployment.configs.properties.Property;

import java.util.Optional;

public interface PropertyFetcher {
    Optional<Property> getProperty(String key, Component component, String environment);
}
