package io.microconfig.environment;

public interface EnvironmentParser<T> {
    Environment parse(String name, T content);
}
