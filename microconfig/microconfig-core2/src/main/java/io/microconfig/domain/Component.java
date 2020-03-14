package io.microconfig.domain;

public interface Component {
    String getName();

    String getEnvironment();

    CompositeComponentConfiguration getPropertiesFor(ConfigTypeFilter filter);
}