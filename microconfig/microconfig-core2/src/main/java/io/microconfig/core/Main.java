package io.microconfig.core;

import io.microconfig.core.service.factory.MicroconfigFactory;

import java.io.File;

import static io.microconfig.core.domain.impl.PropertiesSerializers.toFile;

public class Main {
    public static void main(String[] args) {
        File sourceRoot = new File("/Users/u16805899/Desktop/projects/lm-configs/repo");
        File resultRoot = new File(sourceRoot, "build");

        MicroconfigFactory.withSourceRoot(sourceRoot)
                .environments().byName("dev")
                .getAllComponents()
                .stream()
                .flatMap(component -> component.buildPropertiesForEachConfigType().stream())
                .forEach(properties -> properties.serialize(toFile(resultRoot)));
    }
}