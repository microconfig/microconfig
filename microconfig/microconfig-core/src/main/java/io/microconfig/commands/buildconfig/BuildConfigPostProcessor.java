package io.microconfig.commands.buildconfig;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;

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