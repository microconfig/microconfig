package deployment.mgmt.init;

import io.microconfig.environment.Component;
import io.microconfig.properties.PropertiesProvider;
import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.update.updater.MgmtProperties;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.microconfig.command.factory.PropertyType.DEPENDENCIES;
import static io.microconfig.properties.Property.withoutTempValues;
import static deployment.mgmt.atrifacts.Artifact.fromMavenString;
import static deployment.util.Logger.*;
import static deployment.util.OsUtil.isWindows;
import static deployment.util.ProcessUtil.executeScript;
import static deployment.util.TimeUtils.secAfter;
import static deployment.util.ZipUtils.unzip;
import static java.lang.System.currentTimeMillis;

@RequiredArgsConstructor
public class EnvDependenciesDownloaderImpl implements EnvDependenciesDownloader {
    private final MgmtProperties mgmtProperties;
    private final DeployFileStructure deployFileStructure;
    private final ComponentGroupService componentGroup;
    private final NexusClient nexusClient;

    @Override
    public void downloadDependencies(String env) {
        if (isWindows()) return;

        PropertiesProvider propertiesProvider = mgmtProperties.getPropertyProvider(DEPENDENCIES);
        Map<String, String> dependencies = withoutTempValues(propertiesProvider.getProperties(Component.byType(DEPENDENCIES.name().toLowerCase()), componentGroup.getEnv()));

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