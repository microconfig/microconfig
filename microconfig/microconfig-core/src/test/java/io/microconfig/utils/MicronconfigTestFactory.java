package io.microconfig.utils;

import io.microconfig.commands.factory.BuildCommands;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.PropertyResolverHolder;

import java.io.File;
import java.net.URL;

import static io.microconfig.commands.factory.ConfigType.SERVICE;
import static java.util.Objects.requireNonNull;

public class MicronconfigTestFactory {
    private static final File rootDir = getClasspathFile("test-props");
    private static final BuildCommands commands = BuildCommands.init(rootDir, new File(rootDir, "output"));
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

    private static File getClasspathFile(String name) {
        URL url = requireNonNull(MicronconfigTestFactory.class.getClassLoader().getResource(name), () -> "File doesnt exists: " + name);
        return new File(url.getFile());
    }
}
