package io.microconfig.domain;


import java.util.List;
import java.util.Set;

public interface EnvironmentRepository {
    List<Environment> all();

    Set<String> environmentNames();

    Environment withName(String name);

    Environment getOrCreateWithName(String name);
}