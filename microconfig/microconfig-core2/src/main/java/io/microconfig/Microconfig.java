package io.microconfig;

import io.microconfig.domain.ConfigTypeRepository;
import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.domain.StatementResolver;
import io.microconfig.domain.impl.configtypes.StandardConfigType;
import io.microconfig.domain.impl.environments.ComponentFactory;
import io.microconfig.domain.impl.environments.repository.ComponentFactoryImpl;
import io.microconfig.domain.impl.environments.repository.FileEnvironmentRepository;
import io.microconfig.domain.impl.properties.repository.FilePropertyRepository;
import io.microconfig.domain.impl.properties.resolvers.expression.ExpressionResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.io.formats.FileSystemIo;
import io.microconfig.io.formats.Io;
import io.microconfig.io.graph.ComponentGraph;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;

import static io.microconfig.domain.impl.configtypes.CompositeConfigTypeRepository.composite;
import static io.microconfig.domain.impl.configtypes.DescriptorConfigTypeRepository.findDescriptorIn;
import static io.microconfig.domain.impl.properties.resolvers.chain.ChainedResolver.chainOf;
import static io.microconfig.io.FileUtils.canonical;
import static io.microconfig.io.formats.factory.ConfigIoServiceFactory.newConfigIoService;
import static io.microconfig.io.graph.CachedComponentGraph.traverseFrom;

@RequiredArgsConstructor
public class Microconfig {
    private final File rootDir;
    @With
    private final Io io;

    public static Microconfig searchConfigsIn(File rootDir) {
        File canonical = canonical(rootDir);
        if (!canonical.exists()) {
            throw new IllegalArgumentException("Root directory doesn't exist: " + rootDir);
        }

        return new Microconfig(canonical, new FileSystemIo());
    }

    public Environment inEnvironment(String name) {
        return environments().withName(name);
    }

    public StatementResolver resolver() {
        return chainOf(
                placeholderResolver(),
                expressionResolver()
        );
    }

    private StatementResolver placeholderResolver() {
        return new PlaceholderResolver(
                new StandardResolveStrategy(environments())
        );
    }

    private StatementResolver expressionResolver() {
        return new ExpressionResolver();
    }

    private EnvironmentRepository environments() {
        return new FileEnvironmentRepository(
                rootDir,
                io,
                componentFactory()
        );
    }

    private ComponentFactory componentFactory() {
        return new ComponentFactoryImpl(
                configTypes(),
                propertyRepository()
        );
    }

    private FilePropertyRepository propertyRepository() {
        return new FilePropertyRepository(
                componentGraph(),
                newConfigIoService(io)
        );
    }

    private ConfigTypeRepository configTypes() {
        return composite(
                findDescriptorIn(rootDir, io),
                StandardConfigType.asRepository()
        );
    }

    private ComponentGraph componentGraph() {
        return traverseFrom(rootDir);
    }
}