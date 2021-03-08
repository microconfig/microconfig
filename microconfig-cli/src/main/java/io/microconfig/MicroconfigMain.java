package io.microconfig;

import io.microconfig.core.MicroconfigRunner;
import io.microconfig.core.properties.Properties;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

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
            Map<String, String> envsToBuild = new HashMap<>();

            if(environments.size()==1 && !environments.stream().findFirst().get().equals("*")){
                envsToBuild.put(environments.stream().findFirst().get(), destinationDir);
            } else {
                BiFunction<String, Map<String,String>, String> stageForBuild = (e, m) -> m.put(e,String.format("%s/%s", destinationDir, e));
                if (environments.contains("*")) {
                    getAllEnvs().forEach(e -> stageForBuild.apply(e,envsToBuild));
                } else {
                    environments.forEach(e -> stageForBuild.apply(e,envsToBuild));
                }
            }

            doBuild(envsToBuild);
        } catch (RuntimeException e) {
            if (stacktrace || e.getMessage() == null) {
                throw e;
            }
            error(e.getMessage());
            exit(-1);
        }
    }

    private Set<String> getAllEnvs(){
        return new MicroconfigRunner(rootDir, null).getMicroconfig().environments().environmentNames();
    }

    private void doBuild(Map<String, String> envsToBuild) {
        envsToBuild.keySet().forEach(e -> {
            long startTime = currentTimeMillis();
            MicroconfigRunner runner = new MicroconfigRunner(rootDir, new File(envsToBuild.get(e)));
            runner.getMicroconfig().environments();
            Properties properties = runner.buildProperties(e, groups, services);
            if (jsonOutput) {
                out.println(toJson(properties.save(asConfigResult())));
            } else {
                properties.save(runner.toFiles());
            }
            announce("\nGenerated [" + e + "] configs in " + (currentTimeMillis() - startTime) + "ms");
        });
    }

    private static void printVersion() {
        String version = readClasspathResource("version.properties").split("\n")[0].split("=")[1].trim();
        info(version);
    }
}