package io.microconfig.commands.factory;

import io.microconfig.commands.BuildPropertiesCommand;
import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.environments.filebased.EnvironmentParserImpl;
import io.microconfig.environments.filebased.FileBasedEnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.files.parser.FileComponentParser;
import io.microconfig.properties.files.provider.ComponentTree;
import io.microconfig.properties.files.provider.ComponentTreeCache;
import io.microconfig.properties.files.provider.FileBasedPropertiesProvider;
import io.microconfig.properties.resolver.PropertyFetcherImpl;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.ResolvedPropertiesProvider;
import io.microconfig.properties.resolver.placeholder.PlaceholderResolver;
import io.microconfig.properties.resolver.specific.EnvSpecificPropertiesProvider;
import io.microconfig.properties.resolver.spel.SpelExpressionResolver;
import io.microconfig.properties.serializer.PropertiesSerializerImpl;
import io.microconfig.properties.serializer.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.CacheFactory.cache;
import static io.microconfig.commands.PropertiesPostProcessor.emptyPostProcessor;

@Getter
@RequiredArgsConstructor
public class BuildCommands {
    private static final String MGMT_DIR = ".mgmt";

    private final ComponentTree componentTree;
    private final EnvironmentProvider environmentProvider;
    private final File componentsDir;

    public static BuildCommands init(File repoDir, File componentsDir) {
        ComponentTree componentTree = ComponentTreeCache.build(repoDir);
        EnvironmentProvider environmentProvider = newEnvProvider(repoDir);
        return new BuildCommands(componentTree, environmentProvider, componentsDir);
    }

    public static EnvironmentProvider newEnvProvider(File repoDir) {
        return cache(new FileBasedEnvironmentProvider(new File(repoDir, "envs"), new EnvironmentParserImpl()));
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type) {
        return newBuildCommand(type, mgmtSerializer(type), emptyPostProcessor());
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type, PropertySerializer propertySerializer) {
        return newBuildCommand(type, propertySerializer, emptyPostProcessor());
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type, PropertiesPostProcessor propertiesPostProcessor) {
        return newBuildCommand(type, mgmtSerializer(type), propertiesPostProcessor);
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type, PropertySerializer propertySerializer, PropertiesPostProcessor propertiesPostProcessor) {
        return new BuildPropertiesCommand(environmentProvider, newPropertiesProvider(type), propertySerializer, propertiesPostProcessor);
    }

    public PropertiesProvider newPropertiesProvider(PropertyType propertyType) {
        PropertiesProvider fileBasedPropertiesProvider = cache(new FileBasedPropertiesProvider(componentTree, propertyType.getExtension(), new FileComponentParser(componentTree.getRepoDirRoot())));
        PropertiesProvider envSpecificPropertiesProvider = cache(new EnvSpecificPropertiesProvider(fileBasedPropertiesProvider, environmentProvider, componentTree, componentsDir));
        PropertyResolver placeholderResolver = cache(new SpelExpressionResolver(cache(new PlaceholderResolver(environmentProvider, new PropertyFetcherImpl(envSpecificPropertiesProvider)))));

        return cache(new ResolvedPropertiesProvider(envSpecificPropertiesProvider, placeholderResolver));
    }

    private PropertySerializer mgmtSerializer(PropertyType propertyType) {
        return new PropertiesSerializerImpl(componentsDir, MGMT_DIR + "/" + propertyType.getResultFile());
    }
}