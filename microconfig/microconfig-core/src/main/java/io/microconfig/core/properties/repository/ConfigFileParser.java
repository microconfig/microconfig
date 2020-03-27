package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.Property;
import lombok.Value;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ConfigFileParser {
    ConfigDefinition parse(File file, String configType, String env);

    @Value
    class ConfigDefinition {
        List<Include> includes;
        Map<String, Property> properties;
    }
}
