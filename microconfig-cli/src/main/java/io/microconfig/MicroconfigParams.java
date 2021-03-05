package io.microconfig;

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
        return new File(parser.valueOr("r", "."));
    }

    public String destinationDir() {
        return parser.valueOr("d", "build");
    }

    public List<String> envs() {
        return parser.listValue("e");
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
}