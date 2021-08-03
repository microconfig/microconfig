package io.microconfig.core.environments.repository;

import io.microconfig.core.environments.EnvironmentRepository;
import lombok.Setter;
import lombok.experimental.Delegate;

public class LazyInitEnvRepository implements EnvironmentRepository {
    @Setter
    @Delegate
    private EnvironmentRepository delegate;
}