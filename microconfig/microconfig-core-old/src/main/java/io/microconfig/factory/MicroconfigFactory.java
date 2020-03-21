package io.microconfig.factory;

import io.microconfig.commands.buildconfigs.BuildConfigPostProcessor;
import io.microconfig.commands.buildconfigs.BuildConfigsCommand;
import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.core.environments.filebased.FileBasedEnvironmentProvider;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.io.io.FileSystemIo;
import io.microconfig.core.properties.io.io.Io;
import io.microconfig.core.properties.io.ioservice.ConfigIoService;
import io.microconfig.core.properties.io.ioservice.properties.PropertiesConfigIoService;
import io.microconfig.core.properties.io.ioservice.selector.ConfigFormatDetectorImpl;
import io.microconfig.core.properties.io.ioservice.selector.ConfigIoServiceSelector;
import io.microconfig.core.properties.io.ioservice.yaml.YamlConfigIoService;
import io.microconfig.core.properties.io.tree.CachedComponentTree;
import io.microconfig.core.properties.io.tree.ComponentTree;
import io.microconfig.core.properties.provider.ComponentParserImpl;
import io.microconfig.core.properties.provider.FileBasedConfigProvider;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.resolver.ResolvedConfigProvider;
import io.microconfig.core.properties.resolver.expression.ExpressionResolver;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolver;
import io.microconfig.core.properties.resolver.placeholder.strategies.component.ComponentResolveStrategy;
import io.microconfig.core.properties.resolver.placeholder.strategies.component.properties.ComponentPropertiesFactory;
import io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy;
import io.microconfig.core.properties.resolver.placeholder.strategies.envdescriptor.properties.EnvDescriptorPropertiesFactory;
import io.microconfig.core.properties.resolver.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.core.properties.serializer.ConfigSerializer;
import io.microconfig.core.properties.serializer.file.FilenameGenerator;
import io.microconfig.core.properties.serializer.file.FilenameGeneratorImpl;
import io.microconfig.core.properties.serializer.file.LegacyFilenameGenerator;
import io.microconfig.core.properties.serializer.file.ToFileConfigSerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.commands.buildconfigs.BuildConfigPostProcessor.emptyPostProcessor;
import static io.microconfig.core.environments.filebased.EnvironmentParserImpl.yamlParser;
import static io.microconfig.core.properties.resolver.placeholder.strategies.composite.CompositeResolveStrategy.composite;
import static io.microconfig.core.properties.resolver.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.core.properties.resolver.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.factory.configtypes.StandardConfigTypes.APPLICATION;
import static io.microconfig.utils.CacheProxy.cache;
import static io.microconfig.utils.CollectionUtils.join;
import static io.microconfig.utils.CollectionUtils.joinToSet;
import static io.microconfig.utils.FileUtils.canonical;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class MicroconfigFactory {
    public static final String ENV_DIR = "envs";

    private final ComponentTree componentTree;
    private final EnvironmentProvider environmentProvider;
    private final ConfigIoService configIoService;
    private final File destinationComponentDir;
    @With
    private final String serviceInnerDir;

    private final Map<String, PropertyResolver> resolverByType;
    @With
    private final List<PlaceholderResolveStrategy> additionalResolvers;

    public static MicroconfigFactory init(File sourcesRootDir, File destinationComponentDir) {
        return init(sourcesRootDir, destinationComponentDir, new FileSystemIo());
    }

    public static MicroconfigFactory init(File sourcesRootDir, File destinationComponentDir, Io io) {
        File fullSourcesRootDir = canonical(sourcesRootDir);

        return new MicroconfigFactory(
                CachedComponentTree.prepare(fullSourcesRootDir),
                newEnvProvider(fullSourcesRootDir, io),
                newConfigIoService(io),
                destinationComponentDir,
                "",
                new TreeMap<>(),
                emptyList()
        );
    }

    public ConfigProvider newConfigProvider(ConfigType configType) {
        ConfigProvider fileBasedProvider = newFileBasedProvider(configType);
        return cache(
                new ResolvedConfigProvider(fileBasedProvider, newResolver(fileBasedProvider, configType))
        );
    }

    public ConfigProvider newFileBasedProvider(ConfigType configType) {
        return cache(
                new FileBasedConfigProvider(configType.getSourceExtensions(), componentTree, new ComponentParserImpl(configIoService))
        );
    }

    public PropertyResolver newResolver(ConfigProvider simpleProvider, ConfigType configType) {
        PropertyResolver resolver = cache(new ExpressionResolver(cache(newPlaceholderResolver(simpleProvider, configType))));
        resolverByType.put(configType.getType(), resolver);
        return resolver;
    }

    public PlaceholderResolver newPlaceholderResolver(ConfigProvider simpleProvider, ConfigType configType) {
        ComponentPropertiesFactory componentProperties = new ComponentPropertiesFactory(componentTree, destinationComponentDir);
        EnvDescriptorPropertiesFactory envProperties = new EnvDescriptorPropertiesFactory();

        PlaceholderResolveStrategy strategy = composite(
                join(
                        asList(
                                systemPropertiesResolveStrategy(),
                                new ComponentResolveStrategy(componentProperties.get()),
                                new EnvDescriptorResolveStrategy(environmentProvider, envProperties.get()),
                                new StandardResolveStrategy(simpleProvider),
                                envVariablesResolveStrategy()
                        ),
                        additionalResolvers
                )
        );

        return new PlaceholderResolver(
                environmentProvider,
                strategy,
                joinToSet(componentProperties.get().keySet(), envProperties.get().keySet()),
                configType.getType(),
                resolverByType
        );
    }

    public BuildConfigsCommand newBuildCommand(ConfigType type) {
        return newBuildCommand(type, emptyPostProcessor());
    }

    public BuildConfigsCommand newBuildCommand(ConfigType type, BuildConfigPostProcessor buildConfigPostProcessor) {
        return new BuildConfigsCommand(
                environmentProvider,
                newConfigProvider(type),
                configSerializer(type),
                buildConfigPostProcessor
        );
    }

    private ConfigSerializer configSerializer(ConfigType configType) {
//        return new DiffSerializer(
        return new ToFileConfigSerializer(
                getFilenameGenerator(configType),
                configIoService
        );
//                configIoService
//        );
    }

    //public for ide plugin
    public FilenameGenerator getFilenameGenerator(ConfigType configType) {
        return new LegacyFilenameGenerator(
                APPLICATION.getType().getResultFileName(),
                new FilenameGeneratorImpl(destinationComponentDir, serviceInnerDir, configType.getResultFileName()),
                environmentProvider
        );
    }

    private static EnvironmentProvider newEnvProvider(File root, Io fileReader) {
        return cache(
                new FileBasedEnvironmentProvider(
                        new File(root, ENV_DIR),
                        yamlParser(),
                        fileReader
                )
        );
    }

    private static ConfigIoService newConfigIoService(Io io) {
        return new ConfigIoServiceSelector(
                cache(new ConfigFormatDetectorImpl(io)),
                new YamlConfigIoService(io),
                new PropertiesConfigIoService(io)
        );
    }
}