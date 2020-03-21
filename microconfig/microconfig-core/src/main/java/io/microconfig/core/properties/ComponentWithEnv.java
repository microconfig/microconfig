package io.microconfig.core.properties;

import lombok.Value;
import lombok.With;

@Value
@With
public class ComponentWithEnv {
    String configType;
    String component;
    String environment;
    //todo add config type?? write test this override with diff types

    @Override
    public String toString() {
        return configType + "::" + component + "[" + environment + "]";
    }
}