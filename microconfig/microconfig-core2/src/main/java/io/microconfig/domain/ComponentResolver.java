package io.microconfig.domain;

public interface ComponentResolver {
    ResolvedComponents forEachConfigType();

    ResolvedComponent forConfigType(ConfigTypeFilter filter);
}
