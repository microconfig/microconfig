package deployment.mgmt.configs.fetch.strategy;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.fetch.ConfigFetcherStrategy;
import deployment.mgmt.configs.service.properties.NexusRepository;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static deployment.util.Logger.announce;
import static deployment.util.ZipUtils.unzip;

@RequiredArgsConstructor
public class NexusConfigStrategy implements ConfigFetcherStrategy {
    private final NexusClient nexusClient;
    private final DeploySettings deploySettings;

    @Override
    public void fetchConfigs(String configVersion, File destination) {
        Artifact configArtifact = deploySettings.getConfigArtifact(configVersion);
        NexusRepository repository = deploySettings.getNexusReleaseRepository();

        announce("Downloading configs " + repository.getUrl() + "/" + configArtifact.toUrlPath(null) + " to " + destination);

        File configZip = nexusClient.download(configArtifact)
                .from(repository)
                .asFile();

        unzip(configZip, destination);
    }

    @Override
    public List<String> newConfigReleases(String service, String currentConfigVersion, boolean includeCurrentVersion) {
        return nexusClient.newVersionsFor(
                deploySettings.getConfigArtifact(currentConfigVersion),
                deploySettings.getNexusReleaseRepository(),
                includeCurrentVersion
        );
    }
}
