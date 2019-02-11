package io.microconfig.environments;

public interface EnvironmentParser<T> {
    Environment parse(String name, T content);
}
