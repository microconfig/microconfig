package io.microconfig.core;

import java.io.File;

import static io.microconfig.core.service.io.serializers.PropertiesSerializers.toFileIn;

public class BuildConfigMain {
    public static void main(String[] args) {
        File sourceRoot = new File("/Users/u16805899/Desktop/projects/lm-configs/repo");
        File destinationRoot = new File(sourceRoot, "build");

        MicroconfigFactory.withSourceRoot(sourceRoot)
                .environments().byName("dev")
                .getAllComponents()
                .stream()
                .flatMap(component -> component.buildPropertiesForEachConfigType().stream())
                .forEach(properties -> properties.serialize(toFileIn(destinationRoot)));
    }
}