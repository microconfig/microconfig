package io.microconfig.domain;

public interface ComponentResolver {
    ResultComponents forEachConfigType();

    ResultComponents forConfigType(ConfigTypeFilter filter);
}