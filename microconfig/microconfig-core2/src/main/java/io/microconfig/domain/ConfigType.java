package io.microconfig.domain;

import java.util.Set;

public interface ConfigType {
    String getType();

    Set<String> getSourceExtensions();

    String getResultFileName();
}
