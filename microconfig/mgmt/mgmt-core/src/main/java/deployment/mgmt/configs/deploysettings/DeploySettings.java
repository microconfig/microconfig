package deployment.mgmt.configs.deploysettings;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.NexusRepository;

public interface DeploySettings {
    Artifact getMgmtArtifactFromConfigs();

    Artifact getCurrentMgmtArtifact();

    void setMgmArtifact(Artifact mavenArtifact);


    Artifact getConfigArtifact(String version);

    ConfigSource getConfigSource();

    void setConfigSource(ConfigSource configSource);


    String getConfigGitUrl();

    void setConfigGitUrl(String configGitUrl);

    String getConfigVersion();

    void setConfigVersion(String branch);


    NexusRepository getNexusReleaseRepository();

    Credentials getNexusCredentials();

    void setNexusCredentials(String credentials);


    boolean strictModeEnabled();

    void strictMode(boolean enable);
}