package io.microconfig.utils;

import io.microconfig.environment.EnvironmentProvider;
import io.microconfig.environment.filebased.FileBasedEnvironmentProvider;
import io.microconfig.environment.filebased.EnvironmentParserImpl;

import java.io.File;

import static io.microconfig.utils.TestUtils.getFile;

public class EnvFactory {
    private static final FileBasedEnvironmentProvider ENVS = new FileBasedEnvironmentProvider(
            new File(getFile("test-props"), "envs"),
            new EnvironmentParserImpl()
    );

    public static EnvironmentProvider newEnvironmentProvider() {
        return ENVS;
    }
}
