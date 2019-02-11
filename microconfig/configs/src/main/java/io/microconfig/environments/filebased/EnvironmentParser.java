package io.microconfig.environments.filebased;

import io.microconfig.environments.Environment;

public interface EnvironmentParser<T> {
    Environment parse(String name, T content);
}
