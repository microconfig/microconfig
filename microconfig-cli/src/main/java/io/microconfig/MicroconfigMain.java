package io.microconfig;

import io.microconfig.core.Microconfig;
import io.microconfig.core.MicroconfigRunner;
import io.microconfig.core.properties.Properties;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.microconfig.core.properties.serializers.ConfigResult.toJson;
import static io.microconfig.core.properties.serializers.PropertySerializers.asConfigResult;
import static io.microconfig.utils.IoUtils.readClasspathResource;
import static io.microconfig.utils.Logger.*;
import static java.lang.System.*;

/**
 * Command line params example: *
 * -r configs/repo -d build -e dev
 * <p>
 * VM speedup params:
 * -XX:TieredStopAtLevel=1
 */
@RequiredArgsConstructor
public class MicroconfigMain {
    private final File rootDir;
    private final String destinationDir;
    private final List<String> envs;
    private final List<String> groups;
    private final List<String> services;
    private final boolean stacktrace;
    private final boolean jsonOutput;

    public static void main(String... args) {
        val params = MicroconfigParams.parse(args);
        if (params.version()) {
            printVersion();
            return;
        }

        File rootDir = params.rootDir();
        String destinationDir = params.destinationDir();
        List<String> envs = params.envs();
        List<String> groups = params.groups();
        List<String> services = params.services();
        boolean stacktrace = params.stacktrace();
        boolean jsonOutput = params.jsonOutput();

        new MicroconfigMain(rootDir, destinationDir, envs, groups, services, stacktrace, jsonOutput).build();
    }

    private void build() {
        try {
            enableLogger(!jsonOutput);
            doBuild(envs.contains("*")
                    ? new MicroconfigRunner(rootDir, null).getMicroconfig().environments().environmentNames()
                    : new HashSet<>(envs));
        } catch (RuntimeException e) {
            if (stacktrace || e.getMessage() == null) {
                throw e;
            }

            error(e.getMessage());
            exit(-1);
        }
    }

    private void doBuild(Set<String> envs) {
        envs.forEach(env -> {
            long startTime = currentTimeMillis();
            MicroconfigRunner runner = new MicroconfigRunner(rootDir, new File(String.format("%s/%s", destinationDir, env)));
            runner.getMicroconfig().environments();
            Properties properties = runner.buildProperties(env, groups, services);
            if (jsonOutput) {
                out.println(toJson(properties.save(asConfigResult())));
            } else {
                properties.save(runner.toFiles());
            }
            announce("\nGenerated [" + env + "] configs in " + (currentTimeMillis() - startTime) + "ms");
        });
    }

    private static void printVersion() {
        String version = readClasspathResource("version.properties").split("\n")[0].split("=")[1].trim();
        info(version);
    }
}