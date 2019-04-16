package io.microconfig.entry.factory;

import java.util.Set;

public interface ConfigType {
    String getType();

    Set<String> getSourceExtensions();

    String getResultFileName();
}