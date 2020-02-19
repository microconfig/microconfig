package io.microconfig.testutils;

import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.resolver.PropertyResolverHolder;
import io.microconfig.factory.MicroconfigFactory;

import java.io.File;

import static io.microconfig.factory.configtypes.StandardConfigTypes.APPLICATION;
import static io.microconfig.factory.configtypes.StandardConfigTypes.PROCESS;
import static io.microconfig.testutils.ClasspathUtils.classpathFile;

public class MicronconfigTestFactory {
    private static final File rootDir = classpathFile("test-props");
    private static final MicroconfigFactory factory = MicroconfigFactory.init(rootDir, new File(rootDir, "output"));
    private static final ConfigProvider configProvider = factory.newConfigProvider(APPLICATION.getType());
    private static final EnvironmentProvider envProvider = factory.getEnvironmentProvider();

    static {
        factory.newConfigProvider(PROCESS.getType());
    }

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