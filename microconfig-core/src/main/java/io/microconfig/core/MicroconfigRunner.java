package io.microconfig.core;

import io.microconfig.core.properties.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.core.properties.serializers.PropertySerializers.*;
import static io.microconfig.core.properties.templates.TemplatesService.resolveTemplatesBy;

@Getter
@RequiredArgsConstructor
public class MicroconfigRunner {
    private final Microconfig microconfig;

    public MicroconfigRunner(File rootDir, File destinationDir) {
        this.microconfig = searchConfigsIn(rootDir)
                .withDestinationDir(destinationDir);
    }

    public void build(String env, List<String> groups, List<String> services) {
        microconfig.inEnvironment(env).findComponentsFrom(groups, services)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver())
                .forEachComponent(resolveTemplatesBy(microconfig.resolver()))
                .save(toFiles());
    }

    private PropertySerializer<File> toFiles() {
        return withLegacySupportSaveTo(toFileIn(microconfig.destinationDir(), withConfigDiff()),
                microconfig.environments()
        );
    }
}