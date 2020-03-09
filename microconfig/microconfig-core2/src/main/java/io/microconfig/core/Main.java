package io.microconfig.core;

import io.microconfig.core.service.factory.MicroconfigFactory;

public class Main {
    public static void main(String[] args) {
        MicroconfigFactory.withSourceRoot("/Users/u16805899/Desktop/projects/lm-configs/repo")
                .environments().byName("dev")
                .getAllComponents()
                .stream()
                .flatMap(component -> component.buildPropertiesForEachConfigType().stream())
                .forEach(component -> component.save().toFile());
    }
}