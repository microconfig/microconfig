package io.microconfig;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.configtypes.impl.StandardConfigTypeRepository;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.environments.impl.repository.FileEnvironmentRepository;
import io.microconfig.core.properties.ComponentFactory;
import io.microconfig.core.properties.StatementResolver;
import io.microconfig.core.properties.impl.ComponentFactoryImpl;
import io.microconfig.core.properties.impl.repository.ComponentGraph;
import io.microconfig.core.properties.impl.repository.FilePropertiesRepository;
import io.microconfig.core.resolvers.expression.ExpressionResolver;
import io.microconfig.core.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.core.resolvers.placeholder.strategies.component.ComponentResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.component.properties.ComponentPropertiesFactory;
import io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy;
import io.microconfig.core.resolvers.placeholder.strategies.envdescriptor.properties.EnvDescriptorPropertiesFactory;
import io.microconfig.core.resolvers.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;

import static io.microconfig.core.configtypes.impl.CompositeConfigTypeRepository.composite;
import static io.microconfig.core.configtypes.impl.CustomConfigTypeRepository.findDescriptorIn;
import static io.microconfig.core.properties.impl.io.selector.ConfigIoFactory.newConfigIo;
import static io.microconfig.core.properties.impl.repository.graph.CachedComponentGraph.traverseFrom;
import static io.microconfig.core.resolvers.chain.ChainedResolver.chainOf;
import static io.microconfig.core.resolvers.placeholder.strategies.composite.CompositeResolveStrategy.composite;
import static io.microconfig.core.resolvers.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.core.resolvers.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
import static io.microconfig.utils.FileUtils.canonical;

@RequiredArgsConstructor
public class Microconfig {
    private final File rootDir;
    @With
    private final FsReader fsReader;

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

    public StatementResolver resolver() {
        return chainOf(
                placeholderResolver(),
                expressionResolver()
        );
    }

    private StatementResolver placeholderResolver() {
        ComponentPropertiesFactory componentProperties = new ComponentPropertiesFactory(componentGraph(), rootDir, null); //todo;
        EnvDescriptorPropertiesFactory envProperties = new EnvDescriptorPropertiesFactory();

        return new PlaceholderResolver(composite(
                systemPropertiesResolveStrategy(),
                new ComponentResolveStrategy(componentProperties.get()),
                new EnvDescriptorResolveStrategy(environments(), envProperties.get()),
                new StandardResolveStrategy(environments()),
                envVariablesResolveStrategy()
        ));
    }

    private StatementResolver expressionResolver() {
        return new ExpressionResolver();
    }

    private EnvironmentRepository environments() {
        return new FileEnvironmentRepository(
                rootDir,
                fsReader,
                componentFactory()
        );
    }

    private ComponentFactory componentFactory() {
        return new ComponentFactoryImpl(
                configTypes(),
                propertyRepository()
        );
    }

    private FilePropertiesRepository propertyRepository() {
        return new FilePropertiesRepository(
                componentGraph(),
                newConfigIo(fsReader)
        );
    }

    private ConfigTypeRepository configTypes() {
        return composite(
                findDescriptorIn(rootDir, fsReader),
                new StandardConfigTypeRepository()
        );
    }

    private ComponentGraph componentGraph() {
        return traverseFrom(rootDir);
    }
}