package io.microconfig.domain;

public interface Component {
    String getName();

    String getEnvironment();

    ConfigBuildResults getPropertiesFor(ConfigTypeFilter filter);
}