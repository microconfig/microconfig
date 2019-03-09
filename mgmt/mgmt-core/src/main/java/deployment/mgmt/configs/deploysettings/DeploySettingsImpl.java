package deployment.mgmt.configs.deploysettings;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.NexusRepository;
import io.microconfig.configs.io.ioservice.ConfigIoService;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.mgmt.atrifacts.Artifact.fromMavenString;
import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.RELEASE;
import static deployment.mgmt.init.InitParams.*;
import static io.microconfig.utils.FileUtils.*;
import static io.microconfig.utils.IoUtils.firstLine;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.info;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

@RequiredArgsConstructor
public class DeploySettingsImpl implements DeploySettings {
    private static final String MGMT_VERSION_PROPERTY = "mgmt.version";

    private final DeployFileStructure deployFileStructure;
    private final ComponentGroupService componentGroupService;
    private final EncryptionService simpleEncryptionService;
    private final ConfigIoService configIo;

    private volatile Credentials nexusCredentialCache;

    @Override
    public Artifact getMgmtArtifactFromConfigs() {
        if (!deployFileStructure.deploy().getGroupDescriptionFile().exists()) return null;

        File mgmtVersionFile = deployFileStructure.configs().getMgmtArtifactFile(componentGroupService.getEnv());
        String version = configIo.read(mgmtVersionFile).propertiesAsMap().get(MGMT_VERSION_PROPERTY);
        return version == null ? null : fromMavenString(version);
    }

    @Override
    public Artifact getCurrentMgmtArtifact() {
        return fromMavenString(readFully(mgmtArtifactFile()));
    }

    @Override
    public void setMgmArtifact(Artifact artifact) {
        write(mgmtArtifactFile(), artifact.getMavenFormatString());
    }

    @Override
    public NexusRepository getNexusReleaseRepository() {
        return new NexusRepository("release", systemPropertyOrFileValue(NEXUS_RELEASE_REPOSITORY, nexusReleaseRepositoryFile(), false), RELEASE);
    }

    @Override
    public String getConfigGitUrl() {
        return systemPropertyOrFileValue(CONFIG_GIT_URL, gitUrlFile(), true);
    }

    @Override
    public void setConfigGitUrl(String configGitUrl) {
        setProperty(CONFIG_GIT_URL, configGitUrl);
        getConfigGitUrl();
    }

    @Override
    public Artifact getConfigArtifact(String version) {
        String line = firstLine(configArtifactFile());
        return fromMavenString(line == null ? "configs:configs:zip:${version}" : line).withNewVersion(version);
    }

    @Override
    public ConfigSource getConfigSource() {
        return ConfigSource.valueOf(systemPropertyOrFileValue(CONFIG_SOURCE, configSourceFile(), false));
    }

    @Override
    public void setConfigSource(ConfigSource configSource) {
        write(configSourceFile(), configSource.name());
    }

    @Override
    public String getConfigVersion() {
        String line = firstLine(configVersionFile());
        return line == null ? "master" : line;
    }

    @Override
    public void setConfigVersion(String configVersion) {
        write(configVersionFile(), configVersion);
    }

    @Override
    public Credentials getNexusCredentials() {
        Credentials credentials = this.nexusCredentialCache; //todo2 to decorator
        if (credentials == null) {
            synchronized (this) {
                credentials = this.nexusCredentialCache;
                if (credentials == null) {
                    credentials = this.nexusCredentialCache = Credentials.parse(systemPropertyOrFileValue(NEXUS_CREDENTIALS, nexusCredentialsFile(), true));
                }
            }
        }
        return credentials;
    }

    @Override
    public void setNexusCredentials(String credentials) {
        if (!credentials.contains(":")) {
            throw new IllegalArgumentException("Nexus credentials must contains login and password separated by ':'. Example 'user:secret'");
        }

        nexusCredentialCache = null;
        setProperty(NEXUS_CREDENTIALS, credentials);
        getNexusCredentials();
    }

    @Override
    public boolean strictModeEnabled() {
        return strictModeFile().exists();
    }

    @Override
    public void strictMode(boolean enable) {
        if (enable) {
            createFile(strictModeFile());
        } else {
            delete(strictModeFile());
        }

        info("Strict mode status: " + enable);
    }

    private String systemPropertyOrFileValue(String propertyName, File file, boolean secretMode) {
        String value = getProperty(propertyName);

        if (value != null) {
            write(file, secretMode ? simpleEncryptionService.encrypt(value) : value);
            info("Saved '" + propertyName + "' value to " + file);
            return value;
        } else {
            String rawValue = firstLine(file);
            return secretMode ? simpleEncryptionService.decrypt(rawValue) : rawValue;
        }
    }


    private File gitUrlFile() {
        return settingsFile("git-url.mgmt");
    }

    private File configVersionFile() {
        return settingsFile("config-version.mgmt");
    }

    private File nexusReleaseRepositoryFile() {
        return settingsFile("nexus-release-repository.mgmt");
    }

    private File nexusCredentialsFile() {
        return settingsFile("nexus-credentials.mgmt");
    }

    private File configSourceFile() {
        return settingsFile("config-source.mgmt");
    }

    private File mgmtArtifactFile() {
        return settingsFile("mgmt-version.mgmt");
    }

    private File configArtifactFile() {
        return settingsFile("config-artifact.mgmt");
    }

    private File strictModeFile() {
        return settingsFile("strict-mode.mgmt");
    }

    private File settingsFile(String name) {
        return new File(deployFileStructure.deploy().getDeploySettingsDir(), name);
    }
}