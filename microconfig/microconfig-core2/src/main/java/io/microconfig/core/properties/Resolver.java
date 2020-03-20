package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

public interface Resolver {
    String resolve(CharSequence line, String env, ConfigType configType);
}