package io.microconfig.core.properties.impl.io;


import io.microconfig.core.properties.Property;

import java.util.List;
import java.util.Map;

public interface ConfigReader {
    List<Property> properties(String configType, String environment);

    Map<String, String> propertiesAsMap();

    Map<String, String> escapeResolvedPropertiesAsMap();

    Map<Integer, String> commentsByLineNumber();
}
