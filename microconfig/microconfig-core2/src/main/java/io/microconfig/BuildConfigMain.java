package io.microconfig;

import io.microconfig.utils.CommandLineParams;

import java.io.File;
import java.util.List;

import static io.microconfig.domain.impl.helpers.ConfigTypeFilters.withName;
import static io.microconfig.domain.impl.helpers.PropertySerializers.toFileIn;
import static io.microconfig.factory.MicroconfigFactory.findConfigsIn;

/**
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 * <p>
 * Command line params example: *
 * root=C:\Projects\config\repo dest=C:\Projects\configs env=cr-dev6
 */
public class BuildConfigMain {
    private static final String ROOT = "root";
    private static final String DEST = "dest";
    private static final String ENV = "env";
    private static final String GROUPS = "groups";
    private static final String SERVICES = "services";

    public static void main(String[] args) {
        CommandLineParams clp = CommandLineParams.parse(args);

        File rootDir = new File(clp.requiredValue(ROOT, "set root=  param (folder with 'components' and 'envs' directories)"));
        File destinationDir = new File(clp.requiredValue(DEST, "set dest= param (folder for config build output)"));

        String env = clp.requiredValue(ENV, "set env=");
        List<String> groups = clp.listValue(GROUPS);
        List<String> components = clp.listValue(SERVICES);
        clp.putToSystem("outputFormat");

        findConfigsIn(rootDir)
                .inEnvironment(env)
                .findComponentsFrom(groups, components)
                .buildProperties().forConfigType(withName("app"))
                .save(toFileIn(destinationDir));
    }
}