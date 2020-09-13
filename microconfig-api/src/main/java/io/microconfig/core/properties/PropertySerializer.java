package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.templates.Template;

import java.util.Collection;
import java.util.List;

public interface PropertySerializer<T> {
    T serialize(Collection<Property> properties,
                List<Template> templates,
                ConfigType configType,
                String componentName,
                String environment);
}