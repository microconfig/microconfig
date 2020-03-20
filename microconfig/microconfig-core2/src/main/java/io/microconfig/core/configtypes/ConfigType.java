package io.microconfig.core.configtypes;

import java.util.Set;

public interface ConfigType {
    String getName();

    Set<String> getSourceExtensions();

    String getResultFileName();
}
