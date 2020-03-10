package io.microconfig.domain;

public interface BuildPropertiesStep {
    ResultComponents forEachConfigType();

    ResultComponents forConfigType(ConfigTypeFilter filter);
}