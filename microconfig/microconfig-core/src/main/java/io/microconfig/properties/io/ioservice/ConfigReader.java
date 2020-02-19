package io.microconfig.properties.io.ioservice;

import io.microconfig.properties.Property;

import java.util.List;
import java.util.Map;

public interface ConfigReader {
    List<Property> properties(String env);

    Map<String, String> propertiesAsMap();

    Map<String, String> escapeResolvedPropertiesAsMap();

    Map<Integer, String> commentsByLineNumber();
}
