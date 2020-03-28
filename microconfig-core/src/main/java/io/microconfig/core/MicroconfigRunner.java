package io.microconfig.core;

import io.microconfig.core.configtypes.StandardConfigType;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.List;

import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static io.microconfig.core.properties.serializers.PropertySerializers.toFileIn;
import static io.microconfig.core.properties.serializers.PropertySerializers.withConfigDiff;
import static io.microconfig.core.properties.templates.CopyTemplatesService.resolveTemplatesBy;

@RequiredArgsConstructor
public class MicroconfigRunner {
    private final File rootDir;
    private final File destinationDir;

    public void build(String env, List<String> groups, List<String> services) {
        val microconfig = searchConfigsIn(rootDir).withDestinationDir(destinationDir);
        microconfig.inEnvironment(env).findComponentsFrom(groups, services)
                .getPropertiesFor(configType(StandardConfigType.APPLICATION))
                .resolveBy(microconfig.resolver())
                .forEachComponent(resolveTemplatesBy(microconfig.resolver()))
                .save(toFileIn(destinationDir, withConfigDiff()));
    }
}