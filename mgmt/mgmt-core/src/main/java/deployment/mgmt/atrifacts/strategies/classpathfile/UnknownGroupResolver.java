package deployment.mgmt.atrifacts.strategies.classpathfile;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.io.File;
import java.util.List;

public interface UnknownGroupResolver {
    List<Artifact> resolve(List<Artifact> artifacts, Artifact parentArtifact,
                           List<NexusRepository> nexusRepositories, File localRepositoryDir);
}
