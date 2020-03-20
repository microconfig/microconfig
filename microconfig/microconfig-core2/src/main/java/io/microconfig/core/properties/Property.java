package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

public interface Property {
    String getKey();

    String getValue();

    PropertySource getSource();

    boolean isTemp();

    Property resolveBy(Resolver resolver, ConfigType configType);
}