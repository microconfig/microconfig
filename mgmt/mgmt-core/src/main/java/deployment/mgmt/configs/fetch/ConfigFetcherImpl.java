package deployment.mgmt.configs.fetch;

import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static deployment.mgmt.configs.deploysettings.ConfigSource.GIT;
import static io.microconfig.utils.FileUtils.*;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.PropertiesUtils.hasSystemFlag;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.System.currentTimeMillis;

@RequiredArgsConstructor
public class ConfigFetcherImpl implements ConfigFetcher {
    private final DeploySettings deploySettings;
    private final DeployFileStructure deployFileStructure;
    private final PropertyService propertyService;

    private final ConfigFetcherStrategy gitStrategy;
    private final ConfigFetcherStrategy nexusStrategy;

    @Override
    public void fetchConfigs(String configVersion) {
        File destination = deployFileStructure.configs().getConfigRepoRootDir();
        if (hasSystemFlag("skipConfigFetch") && dirNotEmpty(destination, 2)) {
            announce("Skipping config fetch");
            return;
        }

        long t = currentTimeMillis();
        fetchAtomically(configVersion, destination);
        announce("Fetched configs in " + secAfter(t));
    }

    @Override
    public List<String> newConfigReleases(String service, boolean includeCurrentVersion) {
        String currentConfigVersion = propertyService.getProcessProperties(service).getConfigVersion();
        return getStrategy().newConfigReleases(service, currentConfigVersion, includeCurrentVersion);
    }

    private void fetchAtomically(String configVersion, File destination) {
        File tempDestination = new File(destination.getParent(), destination.getName() + "_temp");
        truncate(tempDestination);

        try {
            getStrategy().fetchConfigs(configVersion, tempDestination);
        } catch (RuntimeException e) {
            delete(tempDestination);
            throw e;
        }

        delete(destination);
        if (!tempDestination.renameTo(destination)) {
            throw new IllegalStateException("Can't rename temp config dir " + tempDestination);
        }
    }

    private ConfigFetcherStrategy getStrategy() {
        return deploySettings.getConfigSource() == GIT ? gitStrategy : nexusStrategy;
    }
}