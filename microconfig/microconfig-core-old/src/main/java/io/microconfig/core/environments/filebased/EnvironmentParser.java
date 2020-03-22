package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.Environment;

import java.io.File;

public interface EnvironmentParser {
    Environment parse(File envFile, String name, String content);
}