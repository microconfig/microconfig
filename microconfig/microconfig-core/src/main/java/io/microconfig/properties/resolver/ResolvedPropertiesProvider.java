package io.microconfig.properties.resolver;

import io.microconfig.environments.Component;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.utils.StreamUtils.toSortedMap;
import static java.util.function.Function.identity;

@RequiredArgsConstructor
public class ResolvedPropertiesProvider implements PropertiesProvider, ResolverHolder {
    private final PropertiesProvider provider;
    private final PropertyResolver resolver;

    @Override
    public Map<String, Property> getProperties(Component rootComponent, String environment) {
        Map<String, Property> properties = provider.getProperties(rootComponent, environment);
        return resolveProperties(properties, new RootComponent(rootComponent, environment));
    }

    @Override
    public PropertyResolver getResolver() {
        return resolver;
    }

    private Map<String, Property> resolveProperties(Map<String, Property> properties, RootComponent root) {
        return properties.values()
                .stream()
                .map(p -> resolveProperty(p, root))
                .collect(toSortedMap(Property::getKey, identity()));
    }

    private Property resolveProperty(Property property, RootComponent root) {
        String resolvedValue = resolver.resolve(property, root);
        return property.withNewValue(resolvedValue);
    }
}