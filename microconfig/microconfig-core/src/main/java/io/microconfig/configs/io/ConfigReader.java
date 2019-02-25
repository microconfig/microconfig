package io.microconfig.configs.io;

import io.microconfig.configs.Property;

import java.util.List;
import java.util.Map;

public interface ConfigReader {
    List<Property> properties();

    List<String> comments();

    Map<String, String> asMap();
}
