package io.microconfig.core.properties.resolver;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.utils.StreamUtils.toSortedMap;
import static java.util.function.Function.identity;

@RequiredArgsConstructor
public class ResolvedConfigProvider implements ConfigProvider, PropertyResolverHolder {
    private final ConfigProvider delegate;
    private final PropertyResolver resolver;

    @Override
    public Map<String, Property> getProperties(Component rootComponent, String environment) {
        Map<String, Property> properties = delegate.getProperties(rootComponent, environment);
        return resolveAll(properties, new EnvComponent(rootComponent, environment));
    }

    @Override
    public PropertyResolver getResolver() {
        return resolver;
    }

    private Map<String, Property> resolveAll(Map<String, Property> properties, EnvComponent root) {
        return properties.values()
                .stream()
                .map(p -> resolve(p, root))
                .collect(toSortedMap(Property::getKey, identity()));
    }

    private Property resolve(Property property, EnvComponent root) {
        String resolvedValue = resolver.resolve(property, root);
        return property.withNewValue(resolvedValue);
    }
}