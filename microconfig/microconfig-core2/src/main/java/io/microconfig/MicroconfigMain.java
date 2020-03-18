package io.microconfig;

import lombok.val;

import java.io.File;
import java.util.List;

import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.domain.impl.properties.PropertySerializers.toFileIn;

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
        microconfig.inEnvironment(env)
                .inGroups(groups)
                .filterComponents(services)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver())
                .save(toFileIn(destinationDir));
    }
}