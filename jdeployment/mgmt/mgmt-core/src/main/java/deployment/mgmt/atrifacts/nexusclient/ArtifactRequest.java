package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.ArtifactType;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.io.File;
import java.util.List;

import static java.util.List.of;

public interface ArtifactRequest {
    ArtifactRequest withType(ArtifactType artifactType);

    ArtifactRequest from(List<NexusRepository> repositories);

    default ArtifactRequest from(NexusRepository... repositories) {
        return from(of(repositories));
    }

    ArtifactRequest tryFindIn(File localRepoDir);

    ArtifactRequest withEventListener(ArtifactRequestListener listener);

    byte[] asBytes();

    String asString();

    File asFile();

    void to(File file);
}