package io.microconfig.domain.impl.environments.repository;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

@RequiredArgsConstructor
public class EnvironmentDefinition {
    private final String name;
    private final String envIp;
    private final Integer portOffset;
    private final EnvInclude envInclude;
    private final List<ComponentGroupDefinition> groups;

    private final File source;
}
