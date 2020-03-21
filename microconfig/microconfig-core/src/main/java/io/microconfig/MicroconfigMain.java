package io.microconfig;

import lombok.val;

import java.io.File;
import java.util.List;

import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.eachConfigType;
import static io.microconfig.core.properties.impl.PropertySerializers.toFileIn;
import static io.microconfig.utils.Logger.announce;

/**
 * Command line params example: *
 * -r configs/repo -d build -e dev
 * <p>
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 */
//todo update documentation
public class MicroconfigMain {
    public static void main(String[] args) {
        val params = MicroconfigParams.parse(args);

        File rootDir = params.rootDir();
        File destinationDir = params.destinationDir();
        String env = params.env();
        List<String> groups = params.groups();
        List<String> services = params.services();

        val microconfig = searchConfigsIn(rootDir);
        microconfig.inEnvironment(env).findComponentsFrom(groups, services)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver())
                .save(toFileIn(destinationDir));

        announce("\nGenerated configs for [" + env + "] env in " + microconfig.msAfterCreation() + "ms");
    }
}