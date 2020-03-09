package io.microconfig.domain.impl.environment.filebased;


import io.microconfig.domain.Environment;

import java.util.Set;

public interface Environments {
    Set<String> environmentNames();

    Environment byName(String name);
}