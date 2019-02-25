package io.microconfig.commands.factory;

import io.microconfig.commands.BuildConfigCommand;
import io.microconfig.commands.BuildConfigPostProcessor;
import io.microconfig.configs.ConfigProvider;
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
import io.microconfig.configs.serializer.ToFileConfigSerializer;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.environments.filebased.EnvironmentParserSelectorImpl;
import io.microconfig.environments.filebased.FileBasedEnvironmentProvider;
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
    private static final String ENVS_DIR = "envs";

    private final ComponentTree componentTree;
    private final EnvironmentProvider environmentProvider;
    private final File destinationComponentDir;
    @Wither
    private final String serviceInnerDir;
    private final ConfigIoService configIo = new ConfigIoServiceSelector(new YamlConfigIoService(), new PropertiesConfigIoService());

    public static MicroconfigFactory init(File root, File destinationComponentDir) {
        File fullRepoDir = canonical(root);
        ComponentTree componentTree = ComponentTreeCache.build(fullRepoDir);
        EnvironmentProvider environmentProvider = newEnvProvider(fullRepoDir);

        return new MicroconfigFactory(componentTree, environmentProvider, destinationComponentDir, "");
    }

    public ConfigProvider newConfigProvider(ConfigType configType) {
        ConfigProvider fileBasedProvider = cache(
                new FileBasedConfigProvider(componentTree, configType.getConfigExtension(), new ComponentParserImpl(configIo))
        );
        SpecialPropertiesFactory specialProperties = new SpecialPropertiesFactory(componentTree, destinationComponentDir);
        PropertyResolver resolver = newPropertyResolver(fileBasedProvider, specialProperties);
        return cache(new ResolvedConfigProvider(fileBasedProvider, resolver));
    }

    private PropertyResolver newPropertyResolver(ConfigProvider fileBasedProvider, SpecialPropertiesFactory specialProperties) {
        return cache(
                new SpelExpressionResolver(
                        cache(new PlaceholderResolver(
                                        environmentProvider,
                                        composite(
                                                systemPropertiesResolveStrategy(),
                                                new StandardResolveStrategy(fileBasedProvider),
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
        return new BuildConfigCommand(environmentProvider, newConfigProvider(type), configSerializer(type), buildConfigPostProcessor);
    }

    private static EnvironmentProvider newEnvProvider(File repoDir) {
        return cache(new FileBasedEnvironmentProvider(new File(repoDir, ENVS_DIR), new EnvironmentParserSelectorImpl(jsonParser(), yamlParser())));
    }

    private ConfigSerializer configSerializer(ConfigType configType) {
        ToFileConfigSerializer serializer = new ToFileConfigSerializer(destinationComponentDir, serviceInnerDir + "/" + configType.getResultFileName(), configIo);
        return new ConfigDiffSerializer(serializer, configIo);
    }
}