package io.microconfig.domain;

public interface Property {
    String getKey();

    String getValue();

    boolean isTemp();
}
