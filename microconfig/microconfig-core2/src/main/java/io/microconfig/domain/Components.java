package io.microconfig.domain;

import java.util.List;

public interface Components {
    List<Component> asList();

    BuildPropertiesStep buildProperties();
}