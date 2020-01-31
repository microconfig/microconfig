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
        if (endsWith(JSON_EXT, envFile)) return jsonParser;
        if (endsWith(YAML_EXT, envFile)) return yamlParser;

        throw new IllegalArgumentException("Env file must be in " + JSON_EXT + " or " + YAML_EXT + " format. Actual: " + envFile);
    }

    private boolean endsWith(String jsonExt, File envFile) {
        return envFile.getName().endsWith(jsonExt);
    }

    @Override
    public List<String> supportedFormats() {
        return asList(JSON_EXT, YAML_EXT);
    }
}
