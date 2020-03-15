package io.microconfig;

import io.microconfig.domain.ConfigTypes;
import io.microconfig.domain.Environment;
import io.microconfig.domain.Environments;
import io.microconfig.domain.StatementResolver;
import io.microconfig.domain.impl.configtype.StandardConfigType;
import io.microconfig.domain.impl.configtype.YamlDescriptorConfigTypes;
import io.microconfig.domain.impl.environment.ComponentFactory;
import io.microconfig.domain.impl.environment.provider.ComponentFactoryImpl;
import io.microconfig.domain.impl.environment.provider.EnvironmentParserImpl;
import io.microconfig.domain.impl.environment.provider.FileBasedEnvironments;
import io.microconfig.domain.impl.properties.repository.FileSystemPropertyRepository;
import io.microconfig.domain.impl.properties.resolvers.expression.ExpressionResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.domain.impl.properties.resolvers.placeholder.strategies.standard.StandardResolveStrategy;
import io.microconfig.io.formats.FileSystemIo;
import io.microconfig.io.formats.Io;
import io.microconfig.io.fsgraph.CachedFileSystemGraph;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;

import static io.microconfig.domain.impl.configtype.CompositeConfigTypes.composite;
import static io.microconfig.domain.impl.properties.resolvers.chain.ChainedResolver.chainOf;
import static io.microconfig.io.FileUtils.canonical;
import static io.microconfig.io.formats.factory.ConfigIoServiceFactory.newConfigIoService;

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

    private Environments environments() {
        return new FileBasedEnvironments(
                rootDir,
                new EnvironmentParserImpl(io, fsComponentFactory())
        );
    }

    private ComponentFactory fsComponentFactory() {
        return new ComponentFactoryImpl(
                configTypes(),
                fsPropertyRepository()
        );
    }

    private FileSystemPropertyRepository fsPropertyRepository() {
        return new FileSystemPropertyRepository(
                fsGraph(),
                newConfigIoService(io)
        );
    }

    private ConfigTypes configTypes() {
        return composite(
                new YamlDescriptorConfigTypes(rootDir, io),
                StandardConfigType.asTypes()
        );
    }

    private FileSystemGraph fsGraph() {
        return CachedFileSystemGraph.prepare(rootDir);
    }
}