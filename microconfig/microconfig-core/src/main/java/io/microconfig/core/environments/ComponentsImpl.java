package io.microconfig.core.environments;

import io.microconfig.core.configtypes.ConfigTypeFilter;
import io.microconfig.core.properties.Properties;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.properties.PropertiesImpl.composite;
import static io.microconfig.utils.StreamUtils.forEach;

@EqualsAndHashCode
@RequiredArgsConstructor
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public Properties getPropertiesFor(ConfigTypeFilter filter) {
        return composite(
                forEach(components.parallelStream(), c -> c.getPropertiesFor(filter))
        );
    }

    @Override
    public String toString() {
        return components.toString();
    }
}