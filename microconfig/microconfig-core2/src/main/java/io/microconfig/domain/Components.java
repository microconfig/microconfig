package io.microconfig.domain;

import io.microconfig.domain.Component;
import io.microconfig.domain.BuildPropertiesStep;

import java.util.List;

public interface Components {
    List<Component> asList();

    BuildPropertiesStep buildProperties();
}