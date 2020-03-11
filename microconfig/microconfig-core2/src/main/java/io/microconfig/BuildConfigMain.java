package io.microconfig;

import java.io.File;
import java.util.List;

import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.eachConfigType;
import static io.microconfig.domain.impl.helpers.PropertySerializers.toFileIn;
import static io.microconfig.factory.MicroconfigFactory.searchConfigsIn;

/**
 * Command line params example: *
 * -r C:\Projects\config\repo -d C:\Projects\configs -e dev
 * <p>
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 */
//todo update documentation
public class BuildConfigMain {
    public static void main(String[] args) {
        MicroconfigParams params = MicroconfigParams.parse(args);

        File rootDir = params.rootDir();
        File destinationDir = params.destinationDir();
        String env = params.env();
        List<String> groups = params.groups();
        List<String> services = params.services();

        searchConfigsIn(rootDir)
                .inEnvironment(env).findComponentsFrom(groups, services)
                .buildPropertiesFor(eachConfigType())
                .save(toFileIn(destinationDir));
    }
}