package io.microconfig.domain;

public interface Component {
    String getAlias();

    String getType();

    String getEnvironment();

    CompositeComponentConfiguration getPropertiesFor(ConfigTypeFilter filter);
}