package io.microconfig.domain.impl.environment;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentResolver;

import java.util.List;

public interface Components {
    List<Component> asList();

    ComponentResolver buildProperties();
}