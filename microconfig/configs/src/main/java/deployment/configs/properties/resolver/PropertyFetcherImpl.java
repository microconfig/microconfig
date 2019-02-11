package deployment.configs.properties.resolver;

import deployment.configs.environment.Component;
import deployment.configs.properties.PropertiesProvider;
import deployment.configs.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class PropertyFetcherImpl implements PropertyFetcher {
    private final PropertiesProvider propertiesProvider;

    @Override
    public Optional<Property> getProperty(String key, Component component, String environment) {
        Map<String, Property> properties = propertiesProvider.getProperties(component, environment);
        return ofNullable(properties.get(key));
    }
}