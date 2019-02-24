package io.microconfig.utils;

import io.microconfig.commands.factory.MicroconfigFactory;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.PropertyResolverHolder;

import java.io.File;

import static io.microconfig.commands.factory.ConfigType.SERVICE;
import static io.microconfig.utils.ClasspathUtils.classpathFile;

public class MicronconfigTestFactory {
    private static final File rootDir = classpathFile("test-props");
    private static final MicroconfigFactory commands = MicroconfigFactory.init(rootDir, new File(rootDir, "output"));
    private static final PropertiesProvider propProvider = commands.newPropertiesProvider(SERVICE);
    private static final EnvironmentProvider envProvider = commands.getEnvironmentProvider();

    public static EnvironmentProvider getEnvProvider() {
        return envProvider;
    }

    public static PropertiesProvider getPropertyProvider() {
        return propProvider;
    }

    public static PropertyResolver getPropertyResolver() {
        return ((PropertyResolverHolder) propProvider).getResolver();
    }
}
