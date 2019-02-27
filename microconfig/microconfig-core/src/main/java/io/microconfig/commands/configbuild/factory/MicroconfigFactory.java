package io.microconfig.commands.configbuild.factory;

import io.microconfig.commands.configbuild.BuildConfigCommand;
import io.microconfig.commands.configbuild.BuildConfigPostProcessor;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.properties.PropertiesConfigIoService;
import io.microconfig.configs.files.io.selector.ConfigFormatDetectorImpl;
import io.microconfig.configs.files.io.selector.ConfigIoServiceSelector;
import io.microconfig.configs.files.io.yaml.YamlConfigIoService;
import io.microconfig.configs.files.provider.ComponentParserImpl;
import io.microconfig.configs.files.provider.FileBasedConfigProvider;
import io.microconfig.configs.files.tree.ComponentTree;
import io.microconfig.configs.files.tree.ComponentTreeCache;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.ResolvedConfigProvider;
import io.microconfig.configs.resolver.placeholder.PlaceholderResolver;
import io.microconfig.configs.resolver.placeholder.strategies.component.ComponentResolveStrategy;
import io.microconfig.configs.resolver.placeholder.strategies.component.properties.ComponentPropertiesFactory;
import io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy;
import io.microconfig.configs.resolver.placeholder.strategies.envdescriptor.properties.EnvDescriptorPropertiesFactory;
import io.microconfig.configs.resolver.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.configs.resolver.spel.SpelExpressionResolver;
import io.microconfig.configs.serializer.ConfigSerializer;
import io.microconfig.configs.serializer.DiffSerializer;
import io.microconfig.configs.serializer.FilenameGeneratorImpl;
import io.microconfig.configs.serializer.ToFileConfigSerializer;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.environments.filebased.EnvironmentParserSelectorImpl;
import io.microconfig.environments.filebased.FileBasedEnvironmentProvider;
import io.microconfig.utils.reader.FileReader;
import io.microconfig.utils.reader.FsFileReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

import java.io.File;
import java.util.Set;

import static io.microconfig.commands.configbuild.BuildConfigPostProcessor.emptyPostProcessor;
import static io.microconfig.configs.resolver.placeholder.ResolveStrategy.composite;
import static io.microconfig.configs.resolver.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.configs.resolver.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.environments.filebased.EnvironmentParserImpl.jsonParser;
import static io.microconfig.environments.filebased.EnvironmentParserImpl.yamlParser;
import static io.microconfig.utils.CacheHandler.cache;
import static io.microconfig.utils.CollectionUtils.joinToSet;
import static io.microconfig.utils.FileUtils.canonical;

@Getter
@RequiredArgsConstructor
public class MicroconfigFactory {
    private static final String ENV_DIR = "envs";

    private final ComponentTree componentTree;
    private final EnvironmentProvider environmentProvider;
    private final ConfigIoService configIoService;
    private final File destinationComponentDir;
    @Wither
    private final String serviceInnerDir;

    public static MicroconfigFactory init(File sourcesRootDir, File destinationComponentDir) {
        return init(sourcesRootDir, destinationComponentDir, new FsFileReader());
    }

    public static MicroconfigFactory init(File sourcesRootDir, File destinationComponentDir, FileReader fileReader) {
        File fullSourcesRootDir = canonical(sourcesRootDir);

        return new MicroconfigFactory(
                ComponentTreeCache.prepare(fullSourcesRootDir),
                newEnvProvider(fullSourcesRootDir, fileReader),
                newConfigIoService(fileReader),
                destinationComponentDir,
                ""
        );
    }

    public ConfigProvider newConfigProvider(ConfigType configType) {
        ConfigProvider fileBasedProvider = cache(
                new FileBasedConfigProvider(componentTree, configType.getConfigExtensions(), new ComponentParserImpl(configIoService))
        );
        return cache(
                new ResolvedConfigProvider(fileBasedProvider, newPropertyResolver(fileBasedProvider))
        );
    }

    private PropertyResolver newPropertyResolver(ConfigProvider configProvider) {
        ComponentPropertiesFactory componentProperties = new ComponentPropertiesFactory(componentTree, destinationComponentDir);
        EnvDescriptorPropertiesFactory envProperties = new EnvDescriptorPropertiesFactory();
        Set<String> specialKeys = joinToSet(componentProperties.get().keySet(), envProperties.get().keySet());

        return cache(
                new SpelExpressionResolver(
                        cache(new PlaceholderResolver(
                                        environmentProvider,
                                        composite(
                                                systemPropertiesResolveStrategy(),
                                                new StandardResolveStrategy(configProvider),
                                                new ComponentResolveStrategy(componentProperties.get()),
                                                new EnvDescriptorResolveStrategy(environmentProvider, envProperties.get()),
                                                envVariablesResolveStrategy()
                                        ),
                                        specialKeys
                                )
                        )
                )
        );
    }

    public BuildConfigCommand newBuildCommand(ConfigType type) {
        return newBuildCommand(type, emptyPostProcessor());
    }

    public BuildConfigCommand newBuildCommand(ConfigType type, BuildConfigPostProcessor buildConfigPostProcessor) {
        return new BuildConfigCommand(
                environmentProvider,
                newConfigProvider(type),
                configSerializer(type),
                buildConfigPostProcessor
        );
    }

    private ConfigSerializer configSerializer(ConfigType configType) {
        return new DiffSerializer(
                new ToFileConfigSerializer(
                        new FilenameGeneratorImpl(destinationComponentDir, serviceInnerDir, configType.getResultFileName()),
                        configIoService
                ),
                configIoService
        );
    }

    private static EnvironmentProvider newEnvProvider(File root, FileReader fileReader) {
        return cache(
                new FileBasedEnvironmentProvider(
                        new File(root, ENV_DIR),
                        new EnvironmentParserSelectorImpl(jsonParser(), yamlParser()),
                        fileReader
                )
        );
    }

    private static ConfigIoService newConfigIoService(FileReader fileReader) {
        return new ConfigIoServiceSelector(
                cache(new ConfigFormatDetectorImpl(fileReader)),
                new YamlConfigIoService(fileReader),
                new PropertiesConfigIoService(fileReader)
        );
    }
}