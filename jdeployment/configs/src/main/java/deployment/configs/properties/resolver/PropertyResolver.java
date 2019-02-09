package deployment.configs.properties.resolver;

import deployment.configs.properties.Property;

public interface PropertyResolver {
    String resolve(Property property, RootComponent root);
}
