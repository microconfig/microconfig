package io.microconfig.core.configtypes;

import java.util.List;

public interface ConfigTypeFilter {
    List<ConfigType> selectTypes(List<ConfigType> supportedTypes);
}