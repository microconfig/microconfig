package deployment.configs.properties.resolver;

import deployment.configs.environment.Component;
import deployment.configs.properties.PropertiesProvider;
import deployment.configs.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static deployment.util.StreamUtils.toSortedMap;
import static java.util.function.Function.identity;

@RequiredArgsConstructor
public class ResolvedPropertiesProvider implements PropertiesProvider {
    private final PropertiesProvider provider;
    private final PropertyResolver resolver;

    @Override
    public Map<String, Property> getProperties(Component rootComponent, String environment) {
        Map<String, Property> properties = provider.getProperties(rootComponent, environment);
        return resolveProperties(properties, rootComponent, environment);
    }

    private Map<String, Property> resolveProperties(Map<String, Property> properties, Component rootComponent, String environment) {
        return properties.values().stream()
                .map(p -> resolveProperty(p, rootComponent, environment))
                .collect(toSortedMap(Property::getKey, identity()));
    }

    private Property resolveProperty(Property property, Component rootComponent, String rootComponentEnv) {
        RootComponent root = new RootComponent(rootComponent, rootComponentEnv);

        String resolvedValue = resolver.resolve(property, root);
        return property.withNewValue(resolvedValue);
    }
}
