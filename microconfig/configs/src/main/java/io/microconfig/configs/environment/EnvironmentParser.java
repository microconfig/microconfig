package io.microconfig.configs.environment;

public interface EnvironmentParser<T> {
    Environment parse(String name, T content);
}
