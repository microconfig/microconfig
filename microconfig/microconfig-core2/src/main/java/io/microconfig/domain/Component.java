package io.microconfig.domain;

public interface Component {
    String getName();

    String getEnvironment();

    ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter);
}