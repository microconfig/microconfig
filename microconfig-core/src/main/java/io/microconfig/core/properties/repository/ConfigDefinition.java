package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.Property;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class ConfigDefinition {
    List<Include> includes;
    Map<String, Property> properties;
}
