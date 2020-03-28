package io.microconfig.core;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.List;

import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.core.properties.serializers.PropertySerializers.toFileIn;
import static io.microconfig.core.properties.serializers.PropertySerializers.withConfigDiff;

@RequiredArgsConstructor
public class MicroconfigRunner {
    private final File rootDir;
    private final File destinationDir;

    public void build(String env, List<String> groups, List<String> services) {
        val microconfig = searchConfigsIn(rootDir).withDestinationDir(destinationDir);
        microconfig.inEnvironment(env).findComponentsFrom(groups, services)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver())
                .save(toFileIn(destinationDir, withConfigDiff()));
    }
}