package io.microconfig.core;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.configtypes.StandardConfigTypeRepository;
import io.microconfig.core.environments.ComponentFactory;
import io.microconfig.core.environments.ComponentFactoryImpl;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.environments.repository.FileEnvironmentRepository;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.core.properties.PropertiesFactoryImpl;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.repository.ComponentGraph;
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
import static io.microconfig.core.properties.resolvers.ChainedResolver.chainOf;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.composite.CompositeResolveStrategy.composite;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.utils.CacheProxy.cache;
import static io.microconfig.utils.CollectionUtils.join;
import static io.microconfig.utils.CollectionUtils.joinToSet;
import static io.microconfig.utils.FileUtils.canonical;
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

    private final Dependencies dependencies = new Dependencies();

    public static Microconfig searchConfigsIn(File rootDir) {
        File canonical = canonical(rootDir);
        if (!canonical.exists()) {
            throw new IllegalArgumentException("Root directory doesn't exist: " + rootDir);
        }
        return new Microconfig(canonical, new File(rootDir, "build"), new DumpedFsReader(), emptyList());
    }

    public Environment inEnvironment(String name) {
        return environments().getByName(name);
    }

    public EnvironmentRepository environments() {
        return dependencies.environments();
    }

    public Resolver resolver() {
        return dependencies.resolver();
    }

    public class Dependencies {
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
            return cache(new FileEnvironmentRepository(
                    rootDir,
                    fsReader,
                    componentFactory(),
                    propertiesFactory())
            );
        }

        private ComponentFactory initComponentFactory() {
            return cache(new ComponentFactoryImpl(
                    configTypeRepository(),
                    propertiesFactory()
            ));
        }

        private PropertiesFactory initPropertiesFactory() {
            return cache(new PropertiesFactoryImpl(
                            cache(new FilePropertiesRepository(
                                    componentGraph(),
                                    newConfigIo(fsReader))
                            )
                    )
            );
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
            return traverseFrom(rootDir);
        }
    }
}