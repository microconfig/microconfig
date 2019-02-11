package io.microconfig.configs.utils;

import io.microconfig.configs.environment.EnvironmentProvider;
import io.microconfig.configs.environment.filebased.FileBasedEnvironmentProvider;
import io.microconfig.configs.environment.filebased.EnvironmentParserImpl;

import java.io.File;

import static io.microconfig.configs.utils.TestUtils.getFile;

public class EnvFactory {
    private static final FileBasedEnvironmentProvider ENVS = new FileBasedEnvironmentProvider(
            new File(getFile("test-props"), "envs"),
            new EnvironmentParserImpl()
    );

    public static EnvironmentProvider newEnvironmentProvider() {
        return ENVS;
    }
}
