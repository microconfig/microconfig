package io.microconfig.utils;

import io.microconfig.commands.buildconfig.factory.MicroconfigFactory;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.environments.EnvironmentProvider;

import java.io.File;

import static io.microconfig.commands.buildconfig.factory.StandardConfigType.SERVICE;
import static io.microconfig.utils.ClasspathUtils.classpathFile;

public class MicronconfigTestFactory {
    private static final File rootDir = classpathFile("test-props");
    private static final MicroconfigFactory factory = MicroconfigFactory.init(rootDir, new File(rootDir, "output"));
    private static final ConfigProvider configProvider = factory.newConfigProvider(SERVICE.type());
    private static final EnvironmentProvider envProvider = factory.getEnvironmentProvider();

    public static EnvironmentProvider getEnvProvider() {
        return envProvider;
    }

    public static ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public static PropertyResolver getPropertyResolver() {
        return ((PropertyResolverHolder) configProvider).getResolver();
    }

    public static MicroconfigFactory getFactory() {
        return factory;
    }
}