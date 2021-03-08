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

import static io.microconfig.CommandLineParamParser.printErrorAndExit;
import static io.microconfig.core.properties.serializers.ConfigResult.toJson;
import static io.microconfig.core.properties.serializers.PropertySerializers.asConfigResult;
import static io.microconfig.utils.CollectionUtils.isCollectionEmpty;
import static io.microconfig.utils.IoUtils.readClasspathResource;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.enableLogger;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.StringUtils.isEmpty;
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
    private final String env;
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
        String env = params.env();
        List<String> envs = params.envs();
        List<String> groups = params.groups();
        List<String> services = params.services();
        boolean stacktrace = params.stacktrace();
        boolean jsonOutput = params.jsonOutput();

        if(isEmpty(env) && isCollectionEmpty(envs)){
            printErrorAndExit("set `-e (environment)` or `-envs (env1),(env2)...`");
        }

        new MicroconfigMain(rootDir, destinationDir, env, envs, groups, services, stacktrace, jsonOutput).build();
    }

    private void build() {
        try {
            enableLogger(!jsonOutput);
            Map<String, String> envsToBuild = new HashMap<>();

            //If the user passed in the -e argument, generate to the outer build directory
            if(!isEmpty(env)){
                envsToBuild.put(env,destinationDir);
            }

            //For all -envs arguments, generate to nested {env} directories.
            //Replace 'env' if it is duplicated in both -e and -envs.
            if(!envs.isEmpty()){
                BiFunction<String, Map<String,String>, String> stageForBuild = (e, m) -> m.put(e,String.format("%s/%s", destinationDir, e));
                if (envs.contains("*")) {
                    getAllEnvs().forEach(e -> stageForBuild.apply(e,envsToBuild));
                } else {
                    envs.forEach(e -> stageForBuild.apply(e,envsToBuild));
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