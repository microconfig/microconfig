package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.util.List;

public interface NexusClient {
    ArtifactRequest download(Artifact artifact);

    List<String> newVersionsFor(Artifact artifact, NexusRepository repository, boolean includeCurrentVersion);
}