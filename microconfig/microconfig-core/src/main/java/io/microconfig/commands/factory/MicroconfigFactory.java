package io.microconfig.commands.factory;

import io.microconfig.commands.BuildConfigCommand;
import io.microconfig.commands.BuildConfigPostProcessor;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.files.format.ConfigFormatDetectorImpl;
import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.ConfigIoServiceSelector;
import io.microconfig.configs.files.io.properties.PropertiesConfigIoService;
import io.microconfig.configs.files.io.yaml.YamlConfigIoService;
import io.microconfig.configs.files.parser.ComponentParserImpl;
import io.microconfig.configs.files.provider.ComponentTree;
import io.microconfig.configs.files.provider.ComponentTreeCache;
import io.microconfig.configs.files.provider.FileBasedConfigProvider;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.ResolvedConfigProvider;
import io.microconfig.configs.resolver.placeholder.PlaceholderResolver;
import io.microconfig.configs.resolver.placeholder.strategies.SpecialPropertyResolveStrategy;
import io.microconfig.configs.resolver.placeholder.strategies.StandardResolveStrategy;
import io.microconfig.configs.resolver.placeholder.strategies.specials.SpecialPropertiesFactory;
import io.microconfig.configs.resolver.spel.SpelExpressionResolver;
import io.microconfig.configs.serializer.ConfigDiffSerializer;
import io.microconfig.configs.serializer.ConfigSerializer;
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

import static io.microconfig.commands.BuildConfigPostProcessor.emptyPostProcessor;
import static io.microconfig.configs.resolver.placeholder.strategies.CompositeResolveStrategy.composite;
import static io.microconfig.configs.resolver.placeholder.strategies.MapResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.configs.resolver.placeholder.strategies.MapResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.environments.filebased.EnvironmentParserImpl.jsonParser;
import static io.microconfig.environments.filebased.EnvironmentParserImpl.yamlParser;
import static io.microconfig.utils.CacheHandler.cache;
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
                new FileBasedConfigProvider(componentTree, configType, new ComponentParserImpl(configIoService))
        );
        return cache(
                new ResolvedConfigProvider(fileBasedProvider, newPropertyResolver(fileBasedProvider))
        );
    }

    private PropertyResolver newPropertyResolver(ConfigProvider configProvider) {
        SpecialPropertiesFactory specialProperties = new SpecialPropertiesFactory(componentTree, destinationComponentDir);
        return cache(
                new SpelExpressionResolver(
                        cache(new PlaceholderResolver(
                                        environmentProvider,
                                        composite(
                                                systemPropertiesResolveStrategy(),
                                                new StandardResolveStrategy(configProvider),
                                                new SpecialPropertyResolveStrategy(environmentProvider, specialProperties.specialPropertiesByKeys()),
                                                envVariablesResolveStrategy()
                                        ),
                                        specialProperties.keyNames()
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
        return new ConfigDiffSerializer(
                new ToFileConfigSerializer(
                        new FilenameGeneratorImpl(destinationComponentDir, serviceInnerDir, configType),
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