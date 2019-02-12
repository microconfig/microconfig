package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.ArtifactType;

import java.io.File;

public interface ArtifactRequestListener {
    void alreadyExists(Artifact artifact, ArtifactType artifactType, File file);

    void downloaded(Artifact artifact, ArtifactType artifactType, File file);

    void error(Artifact artifact, ArtifactType artifactType, Exception e);

    static ArtifactRequestListener empty() {
        return new ArtifactRequestListener() {
            @Override
            public void alreadyExists(Artifact artifact, ArtifactType artifactType, File file) {
            }

            @Override
            public void downloaded(Artifact artifact, ArtifactType artifactType, File file) {
            }

            @Override
            public void error(Artifact artifact, ArtifactType artifactType, Exception e) {
            }
        };
    }
}
