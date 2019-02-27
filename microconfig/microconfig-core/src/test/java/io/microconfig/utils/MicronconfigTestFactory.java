package io.microconfig.utils;

import io.microconfig.commands.build.factory.MicroconfigFactory;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.PropertyResolverHolder;
import io.microconfig.environments.EnvironmentProvider;

import java.io.File;

import static io.microconfig.commands.build.factory.StandardConfigType.SERVICE;
import static io.microconfig.utils.ClasspathUtils.classpathFile;

public class MicronconfigTestFactory {
    private static final File rootDir = classpathFile("test-props");
    private static final MicroconfigFactory commands = MicroconfigFactory.init(rootDir, new File(rootDir, "output"));
    private static final ConfigProvider configProvider = commands.newConfigProvider(SERVICE.type());
    private static final EnvironmentProvider envProvider = commands.getEnvironmentProvider();

    public static EnvironmentProvider getEnvProvider() {
        return envProvider;
    }

    public static ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public static PropertyResolver getPropertyResolver() {
        return ((PropertyResolverHolder) configProvider).getResolver();
    }
}
