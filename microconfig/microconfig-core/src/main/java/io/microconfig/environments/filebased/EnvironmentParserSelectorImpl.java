package io.microconfig.environments.filebased;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class EnvironmentParserSelectorImpl implements EnvironmentParserSelector {
    private static final String JSON_EXT = ".json";
    private static final String YAML_EXT = ".yaml";

    private final EnvironmentParser jsonParser;
    private final EnvironmentParser yamlParser;

    @Override
    public EnvironmentParser selectParser(File envFile) {
        if (envFile.getName().endsWith(JSON_EXT)) return jsonParser;
        if (envFile.getName().endsWith(YAML_EXT)) return yamlParser;

        throw new IllegalArgumentException("Env file must be in .json or .yaml format: " + envFile);
    }

    @Override
    public List<String> supportedFormats() {
        return asList(JSON_EXT, YAML_EXT);
    }
}
