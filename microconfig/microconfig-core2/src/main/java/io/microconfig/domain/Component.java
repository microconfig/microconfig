package io.microconfig.domain;

public interface Component {
    String getName();

    ComponentResolver resolveProperties();
}