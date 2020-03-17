package io.microconfig.domain;

import java.util.List;

public interface ComponentFactory {
    Component createComponent(String componentName, String componentType, String environment);

    Components toComponents(List<Component> components);
}