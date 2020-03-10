package io.microconfig.domain.impl.environment.provider;

import io.microconfig.domain.Environment;

import java.io.File;

public interface EnvironmentParser {
    Environment parse(String name, File envFile);
}