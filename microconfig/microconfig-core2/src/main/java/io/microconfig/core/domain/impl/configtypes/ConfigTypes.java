package io.microconfig.core.domain.impl.configtypes;

import java.util.List;

public interface ConfigTypes {
    ConfigType getByName(String name);

    List<ConfigType> all();
}
