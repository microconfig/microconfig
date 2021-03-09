package io.microconfig;

import io.microconfig.core.MicroconfigRunner;
import io.microconfig.core.properties.Properties;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.microconfig.core.properties.serializers.ConfigResult.toJson;
import static io.microconfig.core.properties.serializers.PropertySerializers.asConfigResult;
import static io.microconfig.utils.IoUtils.readClasspathResource;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.enableLogger;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.Logger.info;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;
import static java.lang.System.out;
import static java.util.stream.Collectors.toCollection;

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
    private final Set<String> environments;
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
        Set<String> environments = params.environments();
        List<String> groups = params.groups();
        List<String> services = params.services();
        boolean stacktrace = params.stacktrace();
        boolean jsonOutput = params.jsonOutput();

        new MicroconfigMain(rootDir, destinationDir, environments, groups, services, stacktrace, jsonOutput).build();
    }

    private void build() {
        try {
            enableLogger(!jsonOutput);
            doBuild();
        } catch (RuntimeException e) {
            if (stacktrace || e.getMessage() == null) {
                throw e;
            }
            error(e.getMessage());
            exit(-1);
        }
    }

    private void doBuild() {
        Set<String> envsToBuild = chooseEnvironments();

        envsToBuild.forEach(e -> {
            long startTime = currentTimeMillis();
            String filePath = envsToBuild.size() > 1 ? destinationDir + "/" + e : destinationDir;
            MicroconfigRunner runner = new MicroconfigRunner(rootDir, new File(filePath));
            Properties properties = runner.buildProperties(e, groups, services);
            if (jsonOutput) {
                out.println(toJson(properties.save(asConfigResult())));
            } else {
                properties.save(runner.toFiles());
            }
            announce("\nGenerated [" + e + "] configs in " + (currentTimeMillis() - startTime) + "ms");
        });
    }

    private Set<String> chooseEnvironments() {
        MicroconfigRunner runner = new MicroconfigRunner(rootDir, null);
        if (!environments.contains("*")) {
            return environments.stream()
                    .filter(e -> !e.startsWith("!"))
                    .collect(toCollection(LinkedHashSet::new));
        }
        return runner.getMicroconfig()
                .environments()
                .environmentNames()
                .stream()
                .filter(e -> !environments.contains("!" + e))
                .collect(toCollection(LinkedHashSet::new));
    }

    private static void printVersion() {
        String version = readClasspathResource("version.properties").split("\n")[0].split("=")[1].trim();
        info(version);
    }
}