package io.microconfig;

import io.microconfig.core.MicroconfigRunner;
import lombok.val;

import java.io.File;
import java.util.List;

import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.error;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;

/**
 * Command line params example: *
 * -r configs/repo -d build -e dev
 * <p>
 * VM speedup params:
 * -Xverify:none -XX:TieredStopAtLevel=1
 */
public class MicroconfigMain {

    public static void main(String... args) {
        System.getProperties().setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");
        val params = MicroconfigParams.parse(args);

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
}