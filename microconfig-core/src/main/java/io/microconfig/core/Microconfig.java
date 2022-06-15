package io.microconfig.core;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.configtypes.StandardConfigTypeRepository;
import io.microconfig.core.environments.ComponentFactory;
import io.microconfig.core.environments.ComponentFactoryImpl;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.environments.repository.EnvironmentException;
import io.microconfig.core.environments.repository.FileEnvironmentRepository;
import io.microconfig.core.environments.repository.LazyInitEnvRepository;
import io.microconfig.core.properties.*;
import io.microconfig.core.properties.repository.ComponentGraph;
import io.microconfig.core.properties.repository.EnvProfilesComponentGraph;
import io.microconfig.core.properties.repository.FilePropertiesRepository;
import io.microconfig.core.properties.resolvers.RecursiveResolver;
import io.microconfig.core.properties.resolvers.expression.ExpressionResolver;
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentProperty;
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.ComponentResolveStrategy;
import io.microconfig.core.properties.resolvers.placeholder.strategies.component.properties.ComponentProperties;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvProperty;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.EnvironmentResolveStrategy;
import io.microconfig.core.properties.resolvers.placeholder.strategies.environment.properties.EnvironmentProperties;
import io.microconfig.core.properties.resolvers.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;
import java.util.Map;

import static io.microconfig.core.configtypes.CompositeConfigTypeRepository.composite;
import static io.microconfig.core.configtypes.CustomConfigTypeRepository.findDescriptorIn;
import static io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo;
import static io.microconfig.core.properties.repository.ComponentGraphImpl.traverseFrom;
import static io.microconfig.core.properties.repository.CompositePropertiesRepository.compositeOf;
import static io.microconfig.core.properties.resolvers.ChainedResolver.chainOf;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.composite.CompositeResolveStrategy.composite;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.utils.CacheProxy.cache;
import static io.microconfig.utils.CollectionUtils.join;
import static io.microconfig.utils.CollectionUtils.joinToSet;
import static io.microconfig.utils.FileUtils.canonical;
import static io.microconfig.utils.Logger.enableLogger;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

@Accessors(fluent = true)
@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class Microconfig {
    private final File rootDir;
    @With
    private final File destinationDir;
    @With
    private final FsReader fsReader;
    @With
    private final List<PlaceholderResolveStrategy> additionalPlaceholderResolvers;
    @With
    private final List<PropertiesRepository> additionalPropertiesRepositories;

    private final Dependencies dependencies = new Dependencies();

    public static Microconfig searchConfigsIn(File rootDir) {
        File canonical = canonical(rootDir);
        if (!canonical.exists()) {
            throw new IllegalArgumentException("Root directory doesn't exist: " + rootDir);
        }
        return new Microconfig(canonical, new File(rootDir, "build"), new DumpedFsReader(), emptyList(), emptyList());
    }

    public Environment inEnvironment(String name) {
        Environment env = environments().getByName(name);
        if (env.isAbstract()) {
            throw new EnvironmentException("Can't build abstract environment " + name);
        }
        return env;
    }

    public EnvironmentRepository environments() {
        return dependencies.environments();
    }

    public Resolver resolver() {
        return dependencies.resolver();
    }

    public void logger(boolean enabled) {
        enableLogger(enabled);
    }

    public class Dependencies {
        private final LazyInitEnvRepository lazyEnvironments = new LazyInitEnvRepository();
        @Getter(lazy = true)
        private final EnvironmentRepository environments = initEnvironments();
        @Getter(lazy = true)
        private final ComponentFactory componentFactory = initComponentFactory();
        @Getter(lazy = true)
        private final ConfigTypeRepository configTypeRepository = initConfigTypeRepository();
        @Getter(lazy = true)
        private final PropertiesFactory propertiesFactory = initPropertiesFactory();
        @Getter(lazy = true)
        private final ComponentGraph componentGraph = initComponentGraph();
        @Getter(lazy = true)
        private final Resolver resolver = initResolver();

        private EnvironmentRepository initEnvironments() {
            EnvironmentRepository repo = cache(new FileEnvironmentRepository(
                    rootDir,
                    fsReader,
                    componentFactory(),
                    propertiesFactory())
            );
            lazyEnvironments.setDelegate(repo);
            return repo;
        }

        private ComponentFactory initComponentFactory() {
            return cache(new ComponentFactoryImpl(
                    configTypeRepository(),
                    propertiesFactory()
            ));
        }

        private PropertiesFactory initPropertiesFactory() {
            PropertiesRepository fileRepository = new FilePropertiesRepository(
                    componentGraph(),
                    lazyEnvironments,
                    newConfigIo(fsReader)
            );

            return cache(new PropertiesFactoryImpl(
                    cache(compositeOf(additionalPropertiesRepositories, fileRepository))
            ));
        }

        public Resolver initResolver() {
            return cache(chainOf(
                    initPlaceholderResolver(),
                    new ExpressionResolver()
            ));
        }

        private RecursiveResolver initPlaceholderResolver() {
            Map<String, ComponentProperty> componentSpecialProperties = new ComponentProperties(componentGraph(), environments(), rootDir, destinationDir).get();
            Map<String, EnvProperty> envSpecialProperties = new EnvironmentProperties().get();

            PlaceholderResolveStrategy strategy = cache(composite(join(
                    additionalPlaceholderResolvers,
                    asList(
                            systemPropertiesResolveStrategy(),
                            envVariablesResolveStrategy(),
                            new ComponentResolveStrategy(componentSpecialProperties),
                            new EnvironmentResolveStrategy(environments(), envSpecialProperties),
                            new StandardResolveStrategy(environments())
                    )
            )));

            return new PlaceholderResolver(
                    strategy,
                    joinToSet(componentSpecialProperties.keySet(), envSpecialProperties.keySet())
            );
        }

        private ConfigTypeRepository initConfigTypeRepository() {
            return cache(composite(
                    findDescriptorIn(rootDir, fsReader),
                    new StandardConfigTypeRepository()
            ));
        }

        private ComponentGraph initComponentGraph() {
            ComponentGraph standard = traverseFrom(rootDir);
            return new EnvProfilesComponentGraph(standard, lazyEnvironments);
        }
    }
}