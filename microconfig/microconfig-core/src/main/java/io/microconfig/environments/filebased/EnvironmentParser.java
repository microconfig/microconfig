package io.microconfig.environments.filebased;

import io.microconfig.environments.Environment;

public interface EnvironmentParser {
    Environment parse(String name, String content);
}