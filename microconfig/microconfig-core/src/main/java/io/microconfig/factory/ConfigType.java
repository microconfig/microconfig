package io.microconfig.factory;

import java.util.Set;

public interface ConfigType {
    String getName();

    String getResultFileName();

    Set<String> getConfigExtensions();
}
