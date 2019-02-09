package deployment.mgmt.atrifacts.strategies.nexus;

import deployment.util.FileLogger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;

import java.io.File;
import java.util.List;

import static deployment.mgmt.atrifacts.strategies.nexus.RepositorySystemFactory.newRepositorySystemSession;
import static java.util.stream.Collectors.toList;

public class NexusResolver {
    public List<File> downloadDependencies(String artifact, boolean resolveSingleArtifact,
                                           List<RemoteRepository> repositories, File localRepositoryDir, File logTo) {
        try (FileLogger logger = new FileLogger(logTo)) {
            RepositorySystem system = RepositorySystemFactory.getRepositorySystem();
            RepositorySystemSession session = newRepositorySystemSession(system, localRepositoryDir, logger);

            DefaultArtifact a = new DefaultArtifact(artifact);

            if (resolveSingleArtifact) {
                ArtifactRequest artifactRequest = new ArtifactRequest(a, repositories, "");
                ArtifactResult result = system.resolveArtifact(session, artifactRequest);
                return List.of(result.getArtifact().getFile());
            }

            ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(a, repositories, "");
            ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(a, ""));
            collectRequest.setRepositories(repositories);
            collectRequest.setManagedDependencies(result.getManagedDependencies());

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setCollectRequest(collectRequest);
            DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);

            return dependencyResult.getArtifactResults().stream()
                    .map(ArtifactResult::getArtifact)
                    .map(org.eclipse.aether.artifact.Artifact::getFile)
                    .collect(toList());
        } catch (DependencyResolutionException | ArtifactDescriptorException | ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }
}