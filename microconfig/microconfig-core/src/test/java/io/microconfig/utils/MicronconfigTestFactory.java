package io.microconfig.utils;

import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.environments.filebased.EnvironmentParserImpl;
import io.microconfig.environments.filebased.FileBasedEnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.files.parser.FileComponentParser;
import io.microconfig.properties.files.provider.ComponentTree;
import io.microconfig.properties.files.provider.ComponentTreeCache;
import io.microconfig.properties.files.provider.FileBasedPropertiesProvider;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.ResolvedPropertiesProvider;
import io.microconfig.properties.resolver.placeholder.PlaceholderResolver;
import io.microconfig.properties.resolver.placeholder.strategies.StandardResolveStrategy;
import io.microconfig.properties.resolver.spel.SpelExpressionResolver;

import java.io.File;
import java.net.URL;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

public class MicronconfigTestFactory {
    private static final EnvironmentProvider environmentProvider = new FileBasedEnvironmentProvider(
            getClasspathFile("test-props/envs"),
            new EnvironmentParserImpl()
    );
    private static final File rootDir = getClasspathFile("test-props/components");
    private static final ComponentTree tree = ComponentTreeCache.build(rootDir);
    private static final PropertiesProvider fileProvider = new FileBasedPropertiesProvider(tree, ".properties", new FileComponentParser("components"));
    private static final PropertyResolver resolver = new SpelExpressionResolver(new PlaceholderResolver(environmentProvider, new StandardResolveStrategy(fileProvider), emptySet()));
    private static final PropertiesProvider propertyProvider = new ResolvedPropertiesProvider(fileProvider, resolver);

    public static EnvironmentProvider getEnvironmentProvider() {
        return environmentProvider;
    }

    public static PropertiesProvider getPropertyProvider() {
        return propertyProvider;
    }

    private static File getClasspathFile(String name) {
        URL url = requireNonNull(MicronconfigTestFactory.class.getClassLoader().getResource(name), () -> "File doesnt exists: " + name);
        return new File(url.getFile());
    }
}
