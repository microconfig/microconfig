package deployment.configs.properties.files.parser;

import deployment.configs.environment.Component;

public interface ComponentParser<T> {
    ComponentProperties parse(T t, Component component, String environment);
}
