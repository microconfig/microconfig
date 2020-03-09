package io.microconfig.domain;

import java.util.Collection;

public interface ConfigTypeSupplier {
    ConfigType chooseType(Collection<ConfigType> supportedTypes);
}