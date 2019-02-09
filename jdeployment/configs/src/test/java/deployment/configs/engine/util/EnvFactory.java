package deployment.configs.engine.util;

import deployment.configs.environment.EnvironmentProvider;
import deployment.configs.environment.filebased.FileBasedEnvironmentProvider;
import deployment.configs.environment.filebased.JsonEnvironmentParser;

import java.io.File;

import static deployment.configs.engine.util.TestUtils.getFile;

public class EnvFactory {
    private static final FileBasedEnvironmentProvider ENVS = new FileBasedEnvironmentProvider(
            new File(getFile("test-props"), "envs"), "json",
            new JsonEnvironmentParser()
    );

    public static EnvironmentProvider newEnvironmentProvider() {
        return ENVS;
    }
}
