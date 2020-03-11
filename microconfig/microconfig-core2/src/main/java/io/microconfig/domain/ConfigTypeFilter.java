package io.microconfig.domain;

import java.util.Collection;

public interface ConfigTypeFilter {
    Collection<ConfigType> selectTypes(Collection<ConfigType> supportedTypes);
}