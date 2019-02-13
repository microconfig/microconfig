package io.microconfig.commands;

import java.io.File;
import java.util.List;

import static io.microconfig.commands.factory.BuildPropertiesCommandFactory.newBuildPropertiesCommand;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.msAfter;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;

/**
 * VM params example
 * -Droot=C:/microconfig/configs-layout-example/repo -Ddest=C:/microconfig/configs -Denv=dev -Xverify:none -XX:TieredStopAtLevel=1
 */
public class BuildConfigMain {
    private static final String ENV = "env";
    private static final String GROUP = "group"; //optional param. values: fnd, mc, etc
    private static final String ROOT = "root";
    private static final String DEST = "dest";

    public static void main(String[] args) {
        String env = requireNonNull(getProperty(ENV), "set -Denv");
        List<String> groups = getProperty(GROUP) == null ? emptyList() : List.of(getProperty(GROUP).trim().split(","));

        String root = requireNonNull(getProperty(ROOT), "set -Droot. -Folder with components and envs folders");
        String destinationDir = requireNonNull(getProperty(DEST), "set -Ddest. Folder of result property output");

        List<String> components = args == null ? emptyList() : asList(args);

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