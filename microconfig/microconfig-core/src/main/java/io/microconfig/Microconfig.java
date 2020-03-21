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

@RequiredArgsConstructor
public class Microconfig {
    private final File rootDir;
    @With
    private final FsReader fsReader;
    private final long creationTime = currentTimeMillis();

    public static Microconfig searchConfigsIn(File rootDir) {
        File canonical = canonical(rootDir);
        if (!canonical.exists()) {
            throw new IllegalArgumentException("Root directory doesn't exist: " + rootDir);
        }

        return new Microconfig(canonical, new DumpedFsReader());
    }

    public Environment inEnvironment(String name) {
        return environments().getByName(name);
    }

    public Resolver resolver() {
        return cache(chainOf(
                placeholderResolver(),
                expressionResolver()
        ));
    }

    private RecursiveResolver placeholderResolver() {
        Map<String, ComponentProperty> componentSpecialProperties = new ComponentProperties(componentGraph(), rootDir, null).get();//todo;
        Map<String, EnvProperty> envSpecialProperties = new EnvironmentProperties().get();

        PlaceholderResolveStrategy strategy = cache(composite(
                systemPropertiesResolveStrategy(),
                new ComponentResolveStrategy(componentSpecialProperties),
                new EnvironmentResolveStrategy(environments(), envSpecialProperties),
                new StandardResolveStrategy(environments()),
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

    private EnvironmentRepository environments() {
        return cache(
                new FileEnvironmentRepository(
                        rootDir,
                        fsReader,
                        componentFactory()
                )
        );
    }

    private ComponentFactory componentFactory() {
        return new ComponentFactoryImpl(
                configTypes(),
                componentPropertiesFactory()
        );
    }

    private ConfigTypeRepository configTypes() {
        return cache(
                composite(
                        findDescriptorIn(rootDir, fsReader),
                        new StandardConfigTypeRepository()
                )
        );
    }

    private PropertiesFactory componentPropertiesFactory() {
        return new PropertiesFactoryImpl(
               cache(new FilePropertiesRepository(
                        componentGraph(),
                        newConfigIo(fsReader)
                ))
        );
    }

    private ComponentGraph componentGraph() {
        return traverseFrom(rootDir);
    }

    public long msAfterCreation() {
        return currentTimeMillis() - creationTime;
    }
}