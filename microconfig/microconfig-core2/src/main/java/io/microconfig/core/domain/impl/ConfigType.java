package io.microconfig.core.domain.impl;

import java.util.Set;

public interface ConfigType {
    String getName();

    Set<String> getSourceExtensions();

    String getResultFileName();
}
