package io.microconfig.core.environments;

import java.util.Set;

public interface EnvironmentProvider {
    Set<String> getEnvironmentNames();

    Environment getByName(String name);
}