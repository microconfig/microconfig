package io.microconfig.core.domain.impl;

import io.microconfig.core.domain.ComponentProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class PropertiesProviderImpl implements PropertiesProvider {

    public ComponentProperties buildProperties(String componentName, String componentType, String env) {
        return null;
    }
}