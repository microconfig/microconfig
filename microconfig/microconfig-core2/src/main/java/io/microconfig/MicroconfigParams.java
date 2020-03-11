package io.microconfig;

import io.microconfig.utils.CommandLineParamParser;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

@RequiredArgsConstructor
public class MicroconfigParams {
    private final CommandLineParamParser parser;

    public static MicroconfigParams parse(String... args) {
        return new MicroconfigParams(CommandLineParamParser.parse(args));
    }

    public File rootDir() {
        return new File(parser.requiredValue("r", "set -r param (folder with 'components' and 'envs' directories)"));
    }

    public File destinationDir() {
        return new File(parser.requiredValue("d", "set -d param (folder for config build output)"));
    }

    public String env() {
        return parser.requiredValue("e", "set -e (environment)");
    }

    public List<String> groups() {
        return parser.listValue("g");
    }

    public List<String> services() {
        return parser.listValue("s");
    }
}