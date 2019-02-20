package io.microconfig.commands;

import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.RootComponent;

import java.io.File;
import java.util.Map;

public interface PropertiesPostProcessor {
    void process(RootComponent currentComponent, File destinationDir, Map<String, Property> componentProperties, PropertiesProvider propertiesProvider);

    static PropertiesPostProcessor emptyPostProcessor() {
        return (p1, p2, p3, p4) -> {
        };
    }
}