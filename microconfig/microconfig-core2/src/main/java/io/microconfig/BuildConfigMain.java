package io.microconfig;

import java.io.File;

import static io.microconfig.domain.impl.helpers.PropertiesSerializers.toFileIn;
import static io.microconfig.factory.MicroconfigFactory.withSourceRoot;

public class BuildConfigMain {
    public static void main(String[] args) {
        File sourceRoot = new File("/Users/u16805899/Desktop/projects/lm-configs/repo");
        File destinationRoot = new File(sourceRoot, "build");

        withSourceRoot(sourceRoot)
                .environments().byName("dev")
                .getAllComponents()
                .stream()
                .flatMap(component -> component.resolvePropertiesForEachConfigType().stream())
                .forEach(properties -> properties.serialize(toFileIn(destinationRoot)));
    }
}