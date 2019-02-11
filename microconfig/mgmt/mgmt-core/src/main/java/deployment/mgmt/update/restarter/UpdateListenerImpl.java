package deployment.mgmt.update.restarter;

import deployment.mgmt.factory.MgmtFactory;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Set;

import static deployment.mgmt.configs.deploysettings.ConfigSource.GIT;
import static deployment.mgmt.configs.deploysettings.ConfigSource.NEXUS;
import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.RELEASE;
import static deployment.util.FileUtils.delete;
import static deployment.util.FileUtils.write;
import static deployment.util.Logger.info;

@RequiredArgsConstructor
public class UpdateListenerImpl implements UpdateListener {
    private final MgmtFactory mgmtFactory;

    @Override
    public void onUpdate() {
        info("Executing update listener");
        mgmtFactory.getMgmtScriptGenerator().generateMgmtScript();

        deleteOldJava11();
        generateNewMgmtSettingsFiles();
        renameConfigVersionFile();
    }

    //todo delete after 31.01.19
    private void deleteOldJava11() {
        delete(new File("/home/rpbin/jdk-11/"));
        delete(new File("/home/rpbin/openjdk-linux-11.28.gz"));
    }

    //todo delete after 31.01.19
    private void generateNewMgmtSettingsFiles() {
        File deploySettingsDir = mgmtFactory.getDeployFileStructure().deploy().getDeploySettingsDir();
        if (new File(deploySettingsDir, "git-current-branch.mgmt").exists()) {
            new File(deploySettingsDir, "nexus.mgmt").delete();
            new File(deploySettingsDir, "git-branch.mgmt").delete();
            return;
        }

        new File(deploySettingsDir, "nexus.mgmt")
                .renameTo(new File(deploySettingsDir, "nexus-credentials.mgmt"));

        write(new File(deploySettingsDir, "config-artifact.mgmt"), "configs:configs:zip:${version}");

        write(new File(deploySettingsDir, "nexus-release-repository.mgmt"),
                mgmtFactory.getMgmtProperties().resolveNexusRepositories().stream()
                        .filter(r -> r.getRepositoryType() == RELEASE).findFirst().orElseThrow().getUrl()
        );

        String env = mgmtFactory.getComponentGroupService().getEnv();
        write(new File(deploySettingsDir, "config-source.mgmt"), (Set.of("cr-ift", "cr-psi", "cr-prod", "cr-prod2")
                .contains(env) ? NEXUS : GIT).name());

        new File(deploySettingsDir, "git-branch.mgmt")
                .renameTo(new File(deploySettingsDir, "git-current-branch.mgmt"));
    }

    //todo delete after 31.01.19
    private void renameConfigVersionFile() {
        File dir = mgmtFactory.getDeployFileStructure().deploy().getDeploySettingsDir();
        File original = new File(dir, "git-current-branch.mgmt");
        if (original.exists()) {
            original.renameTo(new File(dir, "config-version.mgmt"));
        }
    }
}