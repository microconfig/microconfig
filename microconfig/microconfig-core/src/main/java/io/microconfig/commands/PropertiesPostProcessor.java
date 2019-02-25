package io.microconfig.commands;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.RootComponent;

import java.io.File;
import java.util.Map;

public interface PropertiesPostProcessor {
    void process(RootComponent currentComponent, File destinationDir, Map<String, Property> componentProperties, ConfigProvider configProvider);

    static PropertiesPostProcessor emptyPostProcessor() {
        return (p1, p2, p3, p4) -> {
        };
    }
}