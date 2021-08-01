package io.microconfig.core.environments.repository;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Setter
public class LazyInitEnvRepository implements EnvironmentRepository {
    private EnvironmentRepository delegate;

    @Override
    public List<Environment> environments() {
        return delegate.environments();
    }

    @Override
    public Set<String> environmentNames() {
        return delegate.environmentNames();
    }

    @Override
    public Environment getByName(String name) {
        return delegate.getByName(name);
    }

    @Override
    public Environment getOrCreateByName(String name) {
        return delegate.getOrCreateByName(name);
    }
}
