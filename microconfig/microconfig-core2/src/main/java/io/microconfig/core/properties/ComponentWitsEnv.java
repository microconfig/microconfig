package io.microconfig.core.properties;

import lombok.Value;
import lombok.With;

@Value
@With
public class ComponentWitsEnv {
    String component;
    String environment;
}