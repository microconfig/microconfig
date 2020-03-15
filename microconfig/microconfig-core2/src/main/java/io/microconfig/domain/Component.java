package io.microconfig.domain;

public interface Component {
    String getName();

    String getType();

    String getEnvironment();

    CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter);
}