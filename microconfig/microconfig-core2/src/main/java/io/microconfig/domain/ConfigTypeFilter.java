package io.microconfig.domain;

import java.util.Collection;

public interface ConfigTypeFilter {
    Collection<ConfigType> filter(Collection<ConfigType> supportedTypes);
}