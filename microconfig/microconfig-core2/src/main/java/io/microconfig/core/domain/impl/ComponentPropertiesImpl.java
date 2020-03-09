package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.ComponentProperties;
import io.microconfig.core.domain.PropertiesSerializer;
import io.microconfig.core.domain.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

@RequiredArgsConstructor
public class ComponentPropertiesImpl implements ComponentProperties {
    private final String componentName;
    private final ConfigType configType;

    private final Map<String, Property> propertyByKey;
    private final ResultFilenameGenerator resultFilenameGenerator;

    @Override
    public String getConfigType() {
        return configType.getName();
    }

    @Override
    public Map<String, Property> propertyByKey() {
        return propertyByKey;
    }

    @Override
    public PropertiesSerializer save() {
        return new PropertiesSerializer() {
            @Override
            public File toFile() {
                return null;
            }

            @Override
            public String asString() {
                return null;
            }
        };
    }
}
