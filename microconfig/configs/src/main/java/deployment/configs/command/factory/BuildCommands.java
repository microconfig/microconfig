package deployment.configs.command.factory;

import deployment.configs.command.BuildPropertiesCommand;
import deployment.configs.command.PropertiesPostProcessor;
import deployment.configs.environment.EnvironmentProvider;
import deployment.configs.environment.filebased.FileBasedEnvironmentProvider;
import deployment.configs.environment.filebased.EnvironmentParserImpl;
import deployment.configs.properties.PropertiesProvider;
import deployment.configs.properties.files.parser.FileComponentParser;
import deployment.configs.properties.files.provider.ComponentTree;
import deployment.configs.properties.files.provider.ComponentTreeCache;
import deployment.configs.properties.files.provider.FileBasedPropertiesProvider;
import deployment.configs.properties.resolver.PropertyFetcherImpl;
import deployment.configs.properties.resolver.PropertyResolver;
import deployment.configs.properties.resolver.ResolvedPropertiesProvider;
import deployment.configs.properties.resolver.placeholder.PlaceholderResolver;
import deployment.configs.properties.resolver.specific.EnvSpecificPropertiesProvider;
import deployment.configs.properties.resolver.spel.SpelExpressionResolver;
import deployment.configs.properties.serializer.PropertiesSerializerImpl;
import deployment.configs.properties.serializer.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.configs.command.PropertiesPostProcessor.emptyPostProcessor;
import static deployment.util.CacheFactory.cache;

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
        File configRoot = componentTree.getRepoDirRoot().getParentFile();
        PropertiesProvider envSpecificPropertiesProvider = cache(new EnvSpecificPropertiesProvider(fileBasedPropertiesProvider, environmentProvider, componentTree, configRoot, componentsDir));
        PropertyResolver placeholderResolver = cache(new SpelExpressionResolver(cache(new PlaceholderResolver(environmentProvider, new PropertyFetcherImpl(envSpecificPropertiesProvider)))));

        return cache(new ResolvedPropertiesProvider(envSpecificPropertiesProvider, placeholderResolver));
    }

    private PropertySerializer mgmtSerializer(PropertyType propertyType) {
        return new PropertiesSerializerImpl(componentsDir, MGMT_DIR + "/" + propertyType.getResultFile());
    }
}