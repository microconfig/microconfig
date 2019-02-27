package deployment.mgmt.update.updater;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.update.restarter.Restarter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.Logger.*;

@RequiredArgsConstructor
public class MgmtAutoUpdaterImpl implements MgmtAutoUpdater {
    private final DeploySettings deploySettings;
    private final DeployFileStructure deployFileStructure;
    private final NexusClient nexus;
    private final Restarter restarter;

    @Override
    public void updateAndRestart(String minVersion, boolean forceUpdateIfSameVersion, String... args) {
        boolean updated = update(minVersion, forceUpdateIfSameVersion);

        if (updated) {
            restart(args);
        }
    }

    @Override
    public void registerRestartCommand(String... args) {
        restarter.registerRestartCommand(args);
    }

    private boolean update(String minVersion, boolean forceUpdateIfSameVersion) {
        try {
            Artifact configArtifact = deploySettings.getMgmtArtifactFromConfigs();
            if (configArtifact == null) {
                if (forceUpdateIfSameVersion) {
                    configArtifact = deploySettings.getCurrentMgmtArtifact();
                } else {
                    warn("Can't read mgmt version from configs. mgmt autoupdate canceled.");
                    return false;
                }
            }

            configArtifact = configArtifact.withMaxVersion(minVersion);
            if (!needUpdate(configArtifact, forceUpdateIfSameVersion)) return false;

            downloadMgmt(configArtifact);
            deploySettings.setMgmArtifact(configArtifact);

            announce("Updated mgmt to " + configArtifact);
            return true;
        } catch (RuntimeException e) {
            error("Mgmt updateSecrets canceled", e);
            return false;
        }
    }

    private boolean needUpdate(Artifact configArtifact, boolean forceUpdateIfSameVersion) {
        Artifact current = deploySettings.getCurrentMgmtArtifact();
        if (current == null) return true;

        return current.olderThan(configArtifact.getVersion(), forceUpdateIfSameVersion);
    }

    private void downloadMgmt(Artifact artifact) {
        announce("Downloading new mgmt " + artifact);

        nexus.download(artifact)
                .from(deploySettings.getNexusReleaseRepository())
                .to(mgmtJar());
    }

    private void restart(String[] args) {
        restarter.restart(mgmtJar(), args);
    }

    private File mgmtJar() {
        return deployFileStructure.deploy().getMgmtJarFile();
    }
}