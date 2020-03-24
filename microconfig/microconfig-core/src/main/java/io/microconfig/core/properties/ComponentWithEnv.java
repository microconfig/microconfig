package io.microconfig.core.properties;

import lombok.Value;
import lombok.With;

@With
@Value
public class ComponentWithEnv {
    String configType;
    String component;
    String environment;

    @Override
    public String toString() {
        return component + "[" + environment + "]";
    }
}