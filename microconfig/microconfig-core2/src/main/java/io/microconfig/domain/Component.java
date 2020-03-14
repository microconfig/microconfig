package io.microconfig.domain;

public interface Component {
    String getName();

    String getEnvironment();

    CompositeCompositeConfigs getPropertiesFor(ConfigTypeFilter filter);
}