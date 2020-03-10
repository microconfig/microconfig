package io.microconfig.domain;

import java.util.Collection;

public interface ConfigTypeFilter {
    ConfigType chooseType(Collection<ConfigType> supportedTypes);
}