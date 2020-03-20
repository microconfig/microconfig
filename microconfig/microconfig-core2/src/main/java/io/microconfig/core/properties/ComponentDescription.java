package io.microconfig.core.properties;

import lombok.Value;

@Value
public class ComponentDescription {
    String componentName;
    String componentType;
    String environment;
}