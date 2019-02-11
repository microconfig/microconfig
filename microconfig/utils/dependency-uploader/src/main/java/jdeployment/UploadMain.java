package microconfig;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.ArtifactType;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.atrifacts.nexusclient.NexusClientImpl;
import deployment.mgmt.atrifacts.nexusclient.RepositoryPriorityServiceImpl;
import deployment.mgmt.configs.deploysettings.DeploySettingsImpl;
import deployment.mgmt.configs.deploysettings.SimpleEncryptionServiceImpl;
import deployment.mgmt.configs.filestructure.DeployFileStructureImpl;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.io.File;
import java.util.List;

import static deployment.mgmt.atrifacts.ArtifactType.JAR;
import static deployment.mgmt.atrifacts.ArtifactType.POM;
import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.DEPENDENCIES;
import static deployment.util.Logger.announce;
import static deployment.util.Logger.info;
import static deployment.util.ProcessUtil.executeScript;
import static deployment.util.PropertiesUtils.getRequiredProperty;
import static deployment.util.PropertiesUtils.hasSystemFlag;

public class UploadMain {
    private final String downloadNexus;
    private final String uploadNexus;
    private final String uploadRepository;
    private final String uploadAuthentication;

    private final NexusClient nexus;

    public UploadMain(String downloadNexus, String uploadNexus, String uploadRepository, String uploadAuthentication, NexusClient nexusClient) {
        this.downloadNexus = downloadNexus + "/content/groups/public";
        this.uploadNexus = uploadNexus + "/service/local/artifact/maven/content";
        this.uploadRepository = uploadRepository;
        this.uploadAuthentication = uploadAuthentication;
        this.nexus = nexusClient;
    }

    private void doUpload(Artifact artifact) {
        if (!hasSystemFlag("onlyJar")) {
            download(artifact.withoutClassifier(), POM);
        }
        download(artifact, artifact.getClassifier() == null ? JAR : ArtifactType.valueOf(artifact.getClassifier().toUpperCase()));
    }

    private void download(Artifact artifact, ArtifactType type) {
        File file = new File(".", artifact.getMavenFormatString().replace(":", "-") + type.extension());

        nexus.download(artifact)
                .withType(type)
                .from(new NexusRepository("nexus", downloadNexus, DEPENDENCIES))
                .to(file);

        String command = "curl -v -k \\\n"
                + " -F r=" + uploadRepository + " \\\n"
                + " -F e=" + type.name().toLowerCase() + " \\\n"
                + " -F g=" + artifact.getGroupId() + " \\\n"
                + " -F a=" + artifact.getArtifactId() + " \\\n"
                + " -F v=" + artifact.getVersion() + " \\\n"
                + " -F p=" + type.name().toLowerCase() + " \\\n"
                + " -F file=@" + file.getName() + " \\\n"
                + " -u " + uploadAuthentication + " \\\n"
                + uploadNexus;

        announce("UPLOADING " + type + " " + artifact);
        announce(command + "\n");

        executeScript(command);

        info("\n");
    }

    public static void main(String[] args) {
        UploadMain uploadMain = new UploadMain(
                getRequiredProperty("downloadNexus"),
                getRequiredProperty("uploadNexus"),
                getRequiredProperty("uploadRepository"),
                getRequiredProperty("uploadAuthentication"),
                new NexusClientImpl(
                        new RepositoryPriorityServiceImpl(List.of("ru")),
                        new DeploySettingsImpl(DeployFileStructureImpl.init(), null, new SimpleEncryptionServiceImpl())
                )
        );

        for (String arg : args) {
            uploadMain.doUpload(Artifact.fromMavenString(arg));
        }
    }
}