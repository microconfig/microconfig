package io.microconfig.core.environments.repository;

import io.microconfig.core.environments.EnvironmentRepository;
import lombok.Setter;
import lombok.experimental.Delegate;

@Setter
public class LazyInitEnvRepository implements EnvironmentRepository {
    @Delegate
    private EnvironmentRepository delegate;
}