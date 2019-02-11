package deployment.mgmt.atrifacts;

import deployment.mgmt.configs.service.properties.ClasspathStrategyType;
import deployment.mgmt.configs.service.properties.MavenSettings;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.io.File;
import java.util.List;

public interface ClasspathStrategy {
    default List<File> downloadDependencies(MavenSettings mavenSettings) {
        return downloadDependencies(mavenSettings, null);
    }

    default List<File> downloadDependencies(MavenSettings mavenSettings, File logTo) {
        return downloadDependencies(
                mavenSettings.getArtifact(),
                mavenSettings.resolveSingleArtifact(),
                mavenSettings.getNexusRepositories(),
                mavenSettings.getLocalRepositoryDir(),
                logTo
        );
    }

    List<File> downloadDependencies(Artifact artifact, boolean resolveSingleArtifact,
                                    List<NexusRepository> nexusRepositories, File localRepositoryDir,
                                    File logTo);

    ClasspathStrategyType getType();
}