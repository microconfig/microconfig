package io.microconfig.domain;

import java.util.List;
import java.util.Set;

public interface EnvironmentRepository {
    List<Environment> environments();

    Set<String> environmentNames();

    Environment getByName(String name);

    Environment getOrCreateByName(String name);
}