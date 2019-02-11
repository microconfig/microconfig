package io.microconfig.environment;

import java.util.Set;

public interface EnvironmentProvider {
    Set<String> getEnvironmentNames();

    Environment getByName(String name);
}