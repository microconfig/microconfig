package io.microconfig.core.configtypes;

import java.util.List;

public interface ConfigTypesFilter {
    List<ConfigType> selectTypes(List<ConfigType> supportedTypes);
}