package io.microconfig.utils;

import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.environments.filebased.EnvironmentParserImpl;
import io.microconfig.environments.filebased.FileBasedEnvironmentProvider;

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
