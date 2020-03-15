package io.microconfig.domain;

public interface ComponentFactory {
    ComponentFactoryContext forEnvironment(String environment);

    interface ComponentFactoryContext {
        Component createComponent(String componentName, String componentType);

        Components components();
    }
}