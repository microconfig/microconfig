package io.microconfig.factory.configtype;

import io.microconfig.domain.ConfigType;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

public interface ConfigTypeProvider {
    List<ConfigType> getTypes();
}