package io.microconfig.domain;

import java.util.List;

public interface Component {
    String getName();

    List<ComponentProperties> buildPropertiesForEachConfigType();

    ComponentProperties buildPropertiesForType(String configType);
}