package io.microconfig.domain.impl.environment.repository;

import io.microconfig.domain.Environment;

import java.io.File;

public interface EnvironmentParser {
    Environment parse(String name, File envFile);

    Environment fakeEnvWithName(String name);
}