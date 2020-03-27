package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.PropertiesFactory;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.StreamUtils.forEach;

@EqualsAndHashCode
@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;
    private final PropertiesFactory propertiesFactory;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public Properties getPropertiesFor(ConfigTypeFilter configType) {
        return propertiesFactory.composite(
                forEach(components.parallelStream(), c -> c.getPropertiesFor(configType))
        );
    }

    @Override
    public String toString() {
        return components.toString();
    }
}