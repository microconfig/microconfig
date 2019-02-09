package deployment.mgmt.atrifacts.strategies.nexus;

import deployment.util.FileLogger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.*;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;

import java.io.File;

import static deployment.util.Logger.error;
import static java.util.Arrays.asList;

class RepositorySystemFactory {
    static RepositorySystem getRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                error(exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system, File localRepository, FileLogger logger) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        session.setDependencyTraverser(new FatArtifactTraverser());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, new LocalRepository(localRepository)));
        session.setTransferListener(new LoggerTransferListener(logger));
        session.setRepositoryListener(new LoggerRepositoryListener(logger));
        session.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector(asList("compile", "runtime"), null),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()
        ));
        session.setDependencyManager(new ClassicDependencyManager());
        session.setSystemProperties(System.getProperties());
        DependencyGraphTransformer transformer =
                new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(),
                        new SimpleOptionalitySelector(), new JavaScopeDeriver());
        new ChainedDependencyGraphTransformer(transformer, new JavaDependencyContextRefiner());
        session.setDependencyGraphTransformer(transformer);
        return session;
    }
}