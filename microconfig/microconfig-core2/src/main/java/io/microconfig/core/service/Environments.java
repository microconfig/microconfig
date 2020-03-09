package io.microconfig.core.service;


import io.microconfig.core.domain.Environment;

import java.util.Set;

public interface Environments {
    Set<String> environmentNames();

    Environment byName(String name);
}