package io.microconfig.domain;

import java.util.List;

public interface Components {
    List<Component> asList();

    Components filterComponents(List<String> names);

    CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter);
}