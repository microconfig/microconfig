package io.microconfig;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.configtypes.impl.StandardConfigTypeRepository;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.environments.impl.ComponentFactory;
import io.microconfig.core.environments.impl.ComponentFactoryImpl;
import io.microconfig.core.environments.impl.repository.FileEnvironmentRepository;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.impl.PropertiesFactoryImpl;
import io.microconfig.core.properties.impl.repository.ComponentGraph;
import io.microconfig.core.properties.impl.repository.ConfigFileParserImpl;
import io.microconfig.core.properties.impl.repository.FilePropertiesRepository;
import io.microconfig.core.resolvers.RecursiveResolver;
import io.microconfig.core.resolvers.expression.ExpressionResolver;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolveStrategy;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentProperty;
import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.component.properties.ComponentProperties;
import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvProperty;
import io.microconfig.core.resolvers.placeholder.strategies.environment.EnvironmentResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.environment.properties.EnvironmentProperties;
import io.microconfig.core.resolvers.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.Map;

import static io.microconfig.core.configtypes.impl.CompositeConfigTypeRepository.composite;
import static io.microconfig.core.configtypes.impl.CustomConfigTypeRepository.findDescriptorIn;
import static io.microconfig.core.properties.impl.io.selector.ConfigIoFactory.newConfigIo;
import static io.microconfig.core.properties.impl.repository.graph.CachedComponentGraph.traverseFrom;
import static io.microconfig.core.resolvers.ChainedResolver.chainOf;
import static io.microconfig.core.resolvers.placeholder.strategies.composite.CompositeResolveStrategy.composite;
import static io.microconfig.core.resolvers.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.core.resolvers.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.utils.CacheProxy.cache;
import static io.microconfig.utils.CollectionUtils.joinToSet;
import static io.microconfig.utils.FileUtils.canonical;
import static java.lang.System.currentTimeMillis;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class Microconfig {
    private final long creationTime = currentTimeMillis();
    private final File rootDir;
    @With
    private final File destinationDir;
    @With
    private final FsReader fsReader;

    private final Dependencies dependencies = new Dependencies();

    public static Microconfig searchConfigsIn(File rootDir) {
        File canonical = canonical(rootDir);
        if (!canonical.exists()) {
            throw new IllegalArgumentException("Root directory doesn't exist: " + rootDir);
        }
        return new Microconfig(canonical, new File(rootDir, "build"), new DumpedFsReader());
    }

    public long msAfterCreation() {
        return currentTimeMillis() - creationTime;
    }

    public Environment inEnvironment(String name) {
        return environments().getByName(name);
    }

    public EnvironmentRepository environments() {
        return dependencies.getEnvironments();
    }

    public Resolver resolver() {
        return cache(chainOf(
                dependencies.getPlaceholderResolver(),
                dependencies.getExpressionResolver()
        ));
    }

    private class Dependencies {
        @Getter(lazy = true)
        private final EnvironmentRepository environments = environments();
        @Getter(lazy = true)
        private final RecursiveResolver placeholderResolver = placeholderResolver();
        @Getter(lazy = true)
        private final RecursiveResolver expressionResolver = expressionResolver();
        @Getter(lazy = true)
        private final ComponentFactory componentFactory = componentFactory();
        @Getter(lazy = true)
        private final ConfigTypeRepository configTypes = configTypes();
        @Getter(lazy = true)
        private final PropertiesFactory componentPropertiesFactory = componentPropertiesFactory();
        @Getter(lazy = true)
        private final ComponentGraph componentGraph = componentGraph();

        private EnvironmentRepository environments() {
            return cache(new FileEnvironmentRepository(
                    rootDir,
                    fsReader,
                    getComponentFactory()
            ));
        }

        private ComponentFactory componentFactory() {
            return cache(new ComponentFactoryImpl(
                    getConfigTypes(),
                    getComponentPropertiesFactory()
            ));
        }

        private PropertiesFactory componentPropertiesFactory() {
            return cache(new PropertiesFactoryImpl(
                    cache(new FilePropertiesRepository(
                                    getComponentGraph(),
                                    cache(new ConfigFileParserImpl(newConfigIo(fsReader)))
                            )
                    )
            ));
        }

        private RecursiveResolver placeholderResolver() {
            Map<String, ComponentProperty> componentSpecialProperties = new ComponentProperties(getComponentGraph(), getEnvironments(), rootDir, destinationDir).get();
            Map<String, EnvProperty> envSpecialProperties = new EnvironmentProperties().get();

            PlaceholderResolveStrategy strategy = cache(composite(
                    systemPropertiesResolveStrategy(),
                    new ComponentResolveStrategy(componentSpecialProperties),
                    new EnvironmentResolveStrategy(getEnvironments(), envSpecialProperties),
                    new StandardResolveStrategy(getEnvironments()),
                    envVariablesResolveStrategy()
            ));

            return new PlaceholderResolver(
                    strategy,
                    joinToSet(componentSpecialProperties.keySet(), envSpecialProperties.keySet())
            );
        }

        private RecursiveResolver expressionResolver() {
            return new ExpressionResolver();
        }

        private ConfigTypeRepository configTypes() {
            return cache(composite(
                    findDescriptorIn(rootDir, fsReader),
                    new StandardConfigTypeRepository()
            ));
        }

        private ComponentGraph componentGraph() {
            return traverseFrom(rootDir);
        }
    }
}