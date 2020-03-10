package io.microconfig.domain;

public interface Component {
    String getName();

    BuildPropertiesStep buildProperties();
}