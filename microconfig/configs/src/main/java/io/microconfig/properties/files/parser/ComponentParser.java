package io.microconfig.properties.files.parser;

import io.microconfig.environments.Component;

public interface ComponentParser<T> {
    ComponentProperties parse(T t, Component component, String environment);
}
