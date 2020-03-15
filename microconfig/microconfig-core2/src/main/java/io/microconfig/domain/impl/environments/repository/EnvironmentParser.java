package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Environment;

import java.io.File;

public interface EnvironmentParser {
    Environment parse(String name, File envFile);

    Environment fakeEnvWithName(String name);
}