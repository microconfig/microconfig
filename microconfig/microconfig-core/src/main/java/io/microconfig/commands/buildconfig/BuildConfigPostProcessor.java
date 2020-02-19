package io.microconfig.commands.buildconfig;

import io.microconfig.properties.ConfigProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.EnvComponent;

import java.io.File;
import java.util.Map;

public interface BuildConfigPostProcessor {
    void process(EnvComponent currentComponent,
                 Map<String, Property> componentProperties, ConfigProvider configProvider,
                 File resultFile);

    static BuildConfigPostProcessor emptyPostProcessor() {
        return (p1, p3, p4, p2) -> {
        };
    }
}