package io.microconfig.core.properties.templates;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.Property;

import java.util.Collection;

public interface CopyTemplatesService {
    void copyTemplates(Collection<Property> properties,
                       ConfigType configType,
                       String componentName,
                       String environment);
}
