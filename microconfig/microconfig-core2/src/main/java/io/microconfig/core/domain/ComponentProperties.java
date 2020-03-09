package io.microconfig.core.domain;

import java.util.Map;

public interface ComponentProperties {
    String getConfigType();

    Map<String, Property> propertyByKey();

    PropertiesSerializer save();
}