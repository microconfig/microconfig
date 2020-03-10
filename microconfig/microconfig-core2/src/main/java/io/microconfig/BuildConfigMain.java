package io.microconfig;

import io.microconfig.utils.CommandLineParams;

import java.io.File;
import java.util.List;

import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.withName;
import static io.microconfig.domain.impl.helpers.PropertySerializers.toFileIn;
import static io.microconfig.factory.MicroconfigFactory.searchConfigsIn;

/**
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 * <p>
 * Command line params example: *
 * root=C:\Projects\config\repo dest=C:\Projects\configs env=cr-dev6
 */
public class BuildConfigMain {
    private static final String ROOT = "r";
    private static final String DEST = "d";
    private static final String ENV = "e";
    private static final String GROUPS = "g";
    private static final String SERVICES = "s";

    public static void main(String[] args) {
        CommandLineParams clp = CommandLineParams.parse(args);

        File rootDir = new File(clp.requiredValue(ROOT, "set -r param (folder with 'components' and 'envs' directories)"));
        File destinationDir = new File(clp.requiredValue(DEST, "set -d param (folder for config build output)"));

        String env = clp.requiredValue(ENV, "set -e (environment)");
        List<String> groups = clp.listValue(GROUPS);
        List<String> services = clp.listValue(SERVICES);

        searchConfigsIn(rootDir)
                .inEnvironment(env).findComponentsFrom(groups, services)
                .buildProperties().forConfigType(withName("app"))
                .save(toFileIn(destinationDir));
    }
}