package io.microconfig.domain;

public interface Component {
    String getName();

    String getType();

    String getEnvironment();

    CompositeComponentConfiguration getPropertiesFor(ConfigTypeFilter filter);
}