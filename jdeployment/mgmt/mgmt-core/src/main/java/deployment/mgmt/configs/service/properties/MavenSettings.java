package deployment.mgmt.configs.service.properties;

import deployment.mgmt.atrifacts.Artifact;

import java.io.File;
import java.util.List;

public interface MavenSettings {
    Artifact getArtifact();

    boolean resolveSingleArtifact();

    File getLocalRepositoryDir();

    List<NexusRepository> getNexusRepositories();

    ClasspathStrategyType getClasspathResolveStrategy();

    void changeArtifactVersion(String version);

    String getOutFileName();

    //todo2 unzip();
}