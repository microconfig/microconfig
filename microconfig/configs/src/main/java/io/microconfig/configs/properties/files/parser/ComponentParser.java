package io.microconfig.configs.properties.files.parser;

import io.microconfig.configs.environment.Component;

public interface ComponentParser<T> {
    ComponentProperties parse(T t, Component component, String environment);
}
