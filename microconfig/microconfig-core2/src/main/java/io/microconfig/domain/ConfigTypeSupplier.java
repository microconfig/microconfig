package io.microconfig.domain;

import java.util.List;

public interface ConfigTypeSupplier {
    ConfigType chooseType(List<ConfigType> supportedTypes);
}