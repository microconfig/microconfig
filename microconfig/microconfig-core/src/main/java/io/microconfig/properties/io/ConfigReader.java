package io.microconfig.properties.io;

import io.microconfig.properties.Property;

import java.util.List;
import java.util.Map;

public interface ConfigReader {
    List<Property> properties();

    List<String> comments();

    Map<String, String> asMap();
}
