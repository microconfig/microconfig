package deployment.mgmt.atrifacts.strategies.nexus;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.ClasspathStrategy;
import deployment.mgmt.configs.service.properties.ClasspathStrategyType;
import deployment.mgmt.configs.service.properties.NexusRepository;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.util.List;

import static deployment.mgmt.configs.service.properties.ClasspathStrategyType.NEXUS;
import static deployment.util.StreamUtils.map;

public class NexusClasspathStrategy implements ClasspathStrategy {
    private final NexusResolver nexusResolver = new NexusResolver();

    @Override
    public List<File> downloadDependencies(Artifact artifact, boolean resolveSingleArtifact, List<NexusRepository> nexusRepositories, File localRepositoryDir, File logTo) {
        List<RemoteRepository> repositories = map(nexusRepositories, r -> new RemoteRepository.Builder(r.getName(), "default", r.getUrl()).build());
        return nexusResolver.downloadDependencies(artifact.getMavenFormatString(), resolveSingleArtifact, repositories, localRepositoryDir, logTo);
    }

    @Override
    public ClasspathStrategyType getType() {
        return NEXUS;
    }
}
