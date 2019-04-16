package deployment.mgmt.init;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.update.updater.MgmtProperties;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.entry.factory.ConfigType;
import io.microconfig.entry.factory.configtypes.ConfigTypeImpl;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static deployment.mgmt.atrifacts.Artifact.fromMavenString;
import static deployment.mgmt.utils.ZipUtils.unzip;
import static io.microconfig.configs.Property.withoutTempValues;
import static io.microconfig.environments.Component.byType;
import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.OsUtil.isWindows;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.System.currentTimeMillis;
import static mgmt.utils.ProcessUtil.executeScript;

@RequiredArgsConstructor
public class EnvDependenciesDownloaderImpl implements EnvDependenciesDownloader {
    private final MgmtProperties mgmtProperties;
    private final DeployFileStructure deployFileStructure;
    private final ComponentGroupService componentGroup;
    private final NexusClient nexusClient;

    @Override
    public void downloadDependencies(String env) {
        if (isWindows()) return;

        String componentName = "dependencies";
        ConfigType dependenciesType = ConfigTypeImpl.byName(componentName);
        ConfigProvider configProvider = mgmtProperties.getConfigProvider(dependenciesType);
        Map<String, String> dependencies = withoutTempValues(configProvider.getProperties(byType(componentName), componentGroup.getEnv()));

        List<NexusRepository> nexusRepositories = new ArrayList<>();
        dependencies.forEach((name, artifactLine) -> {
            try {
                File dependenciesDir = deployFileStructure.deploy().getDependenciesDir();
                Artifact artifact = fromMavenString(artifactLine);
                File artifactFile = new File(dependenciesDir, artifact.simpleFileName());
                if (artifactFile.exists()) return;

                if (nexusRepositories.isEmpty()) {
                    addEnvUserGroupPermission();
                    nexusRepositories.addAll(mgmtProperties.resolveNexusRepositories());
                }
                announce("Downloading env dependency: " + artifactLine + " to " + artifactFile);

                nexusClient.download(artifact)
                        .from(nexusRepositories)
                        .to(artifactFile);

                if (!isWindows()) {
                    artifactFile.setWritable(true, false);
                }

                info("Unzipping " + artifactFile);
                long t = currentTimeMillis();
                unzip(artifactFile);
                info("Unzipped " + artifactFile + " in " + secAfter(t));
            } catch (RuntimeException e) {
                error("Dependency download error: " + name + "," + artifactLine, e);
            }
        });
    }

    private void addEnvUserGroupPermission() {
        if (isWindows()) return;

        String user = deployFileStructure.deploy().getDependenciesUser();
        String script = deployFileStructure.configs().getMgmtScriptsDir() + "/sshpass -p " + user + " ssh -o StrictHostKeyChecking=no " + user + "@localhost 'chmod 770 ~'";
        executeScript(script);
    }
}