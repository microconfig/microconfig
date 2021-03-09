package io.microconfig;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.microconfig.CommandLineParamParser.printErrorAndExit;

@RequiredArgsConstructor
public class MicroconfigParams {
    private final CommandLineParamParser parser;

    public static MicroconfigParams parse(String... args) {
        return new MicroconfigParams(CommandLineParamParser.parse(args));
    }

    public File rootDir() {
        return new File(parser.valueOr("r", "."));
    }

    public String destinationDir() {
        return parser.valueOr("d", "build");
    }

    public List<String> groups() {
        return parser.listValue("g");
    }

    public List<String> services() {
        return parser.listValue("s");
    }

    public boolean stacktrace() {
        return parser.booleanValue("stacktrace");
    }

    public boolean version() {
        return parser.contains("v");
    }

    public boolean jsonOutput() {
        return "json".equals(parser.value("output"));
    }

    public Set<String> environments() {
        Set<String> environments = new LinkedHashSet<>(parser.listValue("envs"));
        String env = parser.value("e");
        if (env != null) {
            if (env.equals("*")) {
                printErrorAndExit("use -envs instead of -e to pass `*` as a value");
            }
            environments.add(env);
        }
        if (environments.isEmpty()) {
            printErrorAndExit("set `-e (environment)` or `-envs (env1),(env2)...`");
        }
        return environments;
    }

    public boolean isSingleEnvBuild() {
        return parser.value("e") != null && !parser.contains("envs");
    }
}