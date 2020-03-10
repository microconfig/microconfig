package io.microconfig.domain;

public interface ComponentResolver {
    ResolvedComponents forEachConfigType();

    ResolvedComponents forConfigType(ConfigTypeFilter filter);
}