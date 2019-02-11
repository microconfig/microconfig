package deployment.configs.properties;

import deployment.configs.environment.Component;

import java.util.Map;

public interface PropertiesProvider {
    Map<String, Property> getProperties(Component component, String environment);
}
