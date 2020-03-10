package io.microconfig;

import java.io.File;

import static io.microconfig.domain.impl.helpers.ConfigTypeSuppliers.fromFileExtension;
import static io.microconfig.domain.impl.helpers.PropertiesSerializers.asString;
import static io.microconfig.factory.MicroconfigFactory.withSourceRoot;

public class BuildConfigMain {
    public static void main(String[] args) {
        File sourceRoot = new File("/Users/u16805899/Desktop/projects/lm-configs/repo");
        File destinationRoot = new File(sourceRoot, "build");

        String result = withSourceRoot(sourceRoot)
                .environments().byName("dev")
                .getComponentByName("some", false)
                .resolvePropertiesForConfigType(fromFileExtension(new File("someFile.yaml")))
                .serialize(asString());
    }
}