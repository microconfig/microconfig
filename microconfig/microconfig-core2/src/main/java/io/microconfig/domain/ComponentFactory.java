package io.microconfig.domain;

public interface ComponentFactory {
    Component createComponent(String componentName, String componentType, String environment);
}