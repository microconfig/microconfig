package io.microconfig.core.properties;

import java.util.List;

public interface ComponentFactory {
    Component createComponent(String componentName, String componentType, String environment);

    Components toComponents(List<Component> components);
}