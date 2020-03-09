package io.microconfig.core.domain.impl.configtypes;

import java.util.Set;

public interface ConfigType {
    String getName();

    Set<String> getSourceExtensions();

    String getResultFileName();
}
