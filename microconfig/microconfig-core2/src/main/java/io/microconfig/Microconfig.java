package io.microconfig;

import io.microconfig.domain.ConfigTypeRepository;
import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.domain.StatementResolver;
import io.microconfig.domain.impl.configtypes.StandardConfigTypeRepository;
import io.microconfig.domain.impl.environments.repository.FileEnvironmentRepository;
import io.microconfig.domain.impl.properties.ComponentFactory;
import io.microconfig.domain.impl.properties.ComponentFactoryImpl;
import io.microconfig.domain.impl.properties.repository.ComponentGraph;
import io.microconfig.domain.impl.properties.repository.FilePropertiesRepository;
import io.microconfig.domain.impl.properties.resolvers.expression.ExpressionResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component.ComponentResolveStrategy;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.component.properties.ComponentPropertiesFactory;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor.EnvDescriptorResolveStrategy;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.envdescriptor.properties.EnvDescriptorPropertiesFactory;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;

import static io.microconfig.domain.impl.configtypes.CompositeConfigTypeRepository.composite;
import static io.microconfig.domain.impl.configtypes.CustomConfigTypeRepository.findDescriptorIn;
import static io.microconfig.domain.impl.properties.io.selector.ConfigIoFactory.newConfigIo;
import static io.microconfig.domain.impl.properties.repository.graph.CachedComponentGraph.traverseFrom;
import static io.microconfig.domain.impl.properties.resolvers.chain.ChainedResolver.chainOf;
import static io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.composite.CompositeResolveStrategy.composite;
import static io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.envVariablesResolveStrategy;
import static io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.systemPropertiesResolveStrategy;
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