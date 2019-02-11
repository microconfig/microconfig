package io.microconfig.configs.command.factory;

import io.microconfig.configs.command.BuildPropertiesCommand;
import io.microconfig.configs.command.PropertiesPostProcessor;
import io.microconfig.configs.environment.EnvironmentProvider;
import io.microconfig.configs.environment.filebased.EnvironmentParserImpl;
import io.microconfig.configs.environment.filebased.FileBasedEnvironmentProvider;
import io.microconfig.configs.properties.PropertiesProvider;
import io.microconfig.configs.properties.files.parser.FileComponentParser;
import io.microconfig.configs.properties.files.provider.ComponentTree;
import io.microconfig.configs.properties.files.provider.ComponentTreeCache;
import io.microconfig.configs.properties.files.provider.FileBasedPropertiesProvider;
import io.microconfig.configs.properties.resolver.PropertyFetcherImpl;
import io.microconfig.configs.properties.resolver.PropertyResolver;
import io.microconfig.configs.properties.resolver.ResolvedPropertiesProvider;
import io.microconfig.configs.properties.resolver.placeholder.PlaceholderResolver;
import io.microconfig.configs.properties.resolver.specific.EnvSpecificPropertiesProvider;
import io.microconfig.configs.properties.resolver.spel.SpelExpressionResolver;
import io.microconfig.configs.properties.serializer.PropertiesSerializerImpl;
import io.microconfig.configs.properties.serializer.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.util.CacheFactory.cache;
import static io.microconfig.configs.command.PropertiesPostProcessor.emptyPostProcessor;

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