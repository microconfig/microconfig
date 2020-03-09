package io.microconfig.core.domain;

public interface Property {
    String getKey();

    String getValue();

    boolean isTemp();
}
