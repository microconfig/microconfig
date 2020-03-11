package io.microconfig.domain;

public interface BuildPropertiesStep {
    ConfigBuildResults forEachConfigType();

    ConfigBuildResults forConfigType(ConfigTypeFilter filter);
}