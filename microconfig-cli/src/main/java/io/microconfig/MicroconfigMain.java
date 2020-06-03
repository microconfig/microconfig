package io.microconfig;

import io.microconfig.core.MicroconfigRunner;
import lombok.val;

import java.io.File;
import java.util.List;

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
public class MicroconfigMain {
    public static void main(String... args) {
        loggerOff();

        val params = MicroconfigParams.parse(args);
        if (params.version()) {
            printVersion();
            return;
        }

        File rootDir = params.rootDir();
        File destinationDir = params.destinationDir();
        String env = params.env();
        List<String> groups = params.groups();
        List<String> services = params.services();

        try {
            long startTime = currentTimeMillis();
            new MicroconfigRunner(rootDir, destinationDir).build(env, groups, services);
            announce("\nGenerated [" + env + "] configs in " + (currentTimeMillis() - startTime) + "ms");
        } catch (RuntimeException e) {
            if (params.stacktrace() || e.getMessage() == null) {
                throw e;
            }

            error(e.getMessage());
            exit(-1);
        }
    }

    private static void loggerOff() {
        getProperties().setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");
    }

    private static void printVersion() {
        String version = readClasspathResource("gradle.properties").split("\n")[0].split("=")[1].trim();
        info(version);
    }
}