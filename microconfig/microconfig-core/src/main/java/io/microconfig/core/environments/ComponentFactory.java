package io.microconfig.core.environments;

public interface ComponentFactory {
    Component createComponent(String name, String type, String environment);
}
