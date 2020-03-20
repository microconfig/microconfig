package io.microconfig.core.properties;

import lombok.Value;
import lombok.With;

@Value
@With
public class ComponentWithEnv {
    String component;
    String environment;
    //todo add config type?? write test this override with diff types
}