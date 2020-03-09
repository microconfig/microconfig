package io.microconfig;

import java.io.File;

import static io.microconfig.factory.MicroconfigFactory.withSourceRoot;
import static io.microconfig.factory.configtype.ConfigTypeSuppliers.configTypeFromFileName;
import static io.microconfig.service.serializers.PropertiesSerializers.asString;

public class BuildConfigMain {
    public static void main(String[] args) {
        File sourceRoot = new File("/Users/u16805899/Desktop/projects/lm-configs/repo");
        File destinationRoot = new File(sourceRoot, "build");

        String result = withSourceRoot(sourceRoot)
                .environments().byName("dev")
                .getComponentByName("some", false)
                .buildPropertiesFor(configTypeFromFileName(new File("someFile.yaml")))
                .serialize(asString());
    }
}