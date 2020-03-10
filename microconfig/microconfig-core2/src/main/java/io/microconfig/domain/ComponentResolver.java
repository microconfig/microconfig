package io.microconfig.domain;

public interface ComponentResolver {
    ResolvedProperties forEachConfigType();

    ResolvedProperties forConfigType(ConfigTypeFilter filter);
}
