package io.microconfig.core.properties;

import lombok.Value;
import lombok.With;

@Value
@With
public class ComponentDescription {
    String componentName;
    String componentType;
    String environment;
}