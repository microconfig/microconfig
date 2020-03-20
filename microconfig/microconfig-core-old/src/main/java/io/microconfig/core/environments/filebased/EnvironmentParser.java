package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.Environment;

public interface EnvironmentParser {
    Environment parse(String name, String content);
}