package io.microconfig.domain;

import java.util.List;

public interface ConfigTypeFilter {
    List<ConfigType> selectTypes(List<ConfigType> supportedTypes);
}