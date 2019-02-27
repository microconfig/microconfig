package io.microconfig.commands.configbuild.entry;

import io.microconfig.commands.Command;
import io.microconfig.commands.CommandContext;
import io.microconfig.utils.CommandLineParams;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.configbuild.factory.BuildConfigCommandFactory.newBuildCommand;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.msAfter;
import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;

/**
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 * <p>
 * Command line params example:
 * root=Z:\Programming\Java\projects\microconfig-projects\configs-layout-example\repo dest=Z:\Programming\Java\projects\microconfig-projects\output env=dev
 * root=C:\Projects\config\repo dest=C:\Projects\configs env=cr-dev6 outputFormat=.yaml
 */
public class BuildConfigMain {
    private static final String ROOT = "root";
    private static final String DEST = "dest";
    private static final String ENV = "env";
    private static final String GROUPS = "groups";
    private static final String SERVICES = "services";

    public static void main(String[] args) {
        CommandLineParams clp = CommandLineParams.parse(args);

        String root = clp.requiredValue(ROOT, "set root=  param (folder with 'components' and 'envs' directories)");
        String destination = clp.requiredValue(DEST, "set dest= param (folder for config build output)");
        String env = clp.requiredValue(ENV, "set env=");
        List<String> groups = clp.listValue(GROUPS);
        List<String> components = clp.listValue(SERVICES);
        clp.putToSystem("outputFormat");

        Command command = newBuildCommand(new File(root), new File(destination));
        execute(command, env, groups, components);
    }

    public static void execute(Command command, String env, List<String> groups, List<String> components) {
        long t = currentTimeMillis();

        if (groups.isEmpty()) {
            command.execute(new CommandContext(env, components));
        } else {
            groups.forEach(group -> command.execute(new CommandContext(env, of(group), components)));
        }

        announce("Generated configs in " + msAfter(t));
    }
}