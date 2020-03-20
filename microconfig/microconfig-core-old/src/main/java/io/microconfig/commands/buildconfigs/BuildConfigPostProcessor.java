package io.microconfig.commands.buildconfigs;

import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.EnvComponent;

import java.io.File;
import java.util.Map;

public interface BuildConfigPostProcessor {
    void process(EnvComponent currentComponent,
                 Map<String, Property> componentProperties,
                 ConfigProvider configProvider,
                 File resultFile);

    static BuildConfigPostProcessor emptyPostProcessor() {
        return (_1, _2, _3, _4) -> {
        };
    }
}