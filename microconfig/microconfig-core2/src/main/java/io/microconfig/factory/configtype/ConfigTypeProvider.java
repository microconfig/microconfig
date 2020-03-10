package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;

import java.util.List;

public interface ConfigTypeProvider {
    List<ConfigType> getTypes();
}