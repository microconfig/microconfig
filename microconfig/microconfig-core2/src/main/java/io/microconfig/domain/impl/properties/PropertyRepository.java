package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;

import java.util.List;

public interface PropertyRepository {
    List<Property> getProperties(String componentName,
                                 String componentType,
                                 String environment,
                                 ConfigType configType);
}