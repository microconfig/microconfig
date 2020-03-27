package io.microconfig;

import io.microconfig.core.MicroconfigRunner;
import lombok.val;

import java.io.File;
import java.util.List;

import static io.microconfig.utils.Logger.announce;
import static java.lang.System.currentTimeMillis;

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

        long startTime = currentTimeMillis();
        new MicroconfigRunner(rootDir, destinationDir).build(env, groups, services);
        announce("\nGenerated [" + env + "] configs in " + (currentTimeMillis() - startTime) + "ms");
    }
}