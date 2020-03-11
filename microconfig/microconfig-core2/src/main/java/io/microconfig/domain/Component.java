package io.microconfig.domain;

public interface Component {
    String getName();

    ConfigBuildResults buildPropertiesFor(ConfigTypeFilter filter);
}