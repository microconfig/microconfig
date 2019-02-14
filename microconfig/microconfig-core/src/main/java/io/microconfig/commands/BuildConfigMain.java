package io.microconfig.commands;

import io.microconfig.utils.CommandLineParams;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.factory.BuildPropertiesCommandFactory.newBuildPropertiesCommand;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.msAfter;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;

/**
 * Command line params example
 * VM speedup params -Xverify:none -XX:TieredStopAtLevel=1
 * root=C:/microconfig/configs-layout-example/repo dest=C:/microconfig/configs env=dev
 */
public class BuildConfigMain {
    private static final String ENV = "env";
    private static final String GROUP = "group"; //optional param. values: fnd, mc, etc
    private static final String ROOT = "root";
    private static final String DEST = "dest";
    private static final String SERVICES = "services";

    public static void main(String[] args) {
        CommandLineParams clp = CommandLineParams.parse(args);

        String env = clp.requiredValue(ENV, "set env=");
        List<String> groups = clp.value(GROUP) == null ? emptyList() : asList(clp.value(GROUP).trim().split(","));

        String root = clp.requiredValue(ROOT, "set root=  param. Folder with components and envs folders");
        String destinationDir = clp.requiredValue(DEST, "set dest= param. Folder of result property output");

        List<String> components = clp.value(SERVICES) == null ? emptyList() : asList(clp.value("services").split(","));

        Command command = newBuildPropertiesCommand(new File(root), new File(destinationDir));
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