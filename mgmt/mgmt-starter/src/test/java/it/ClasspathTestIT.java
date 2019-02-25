package it;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.ClasspathStrategy;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.atrifacts.nexusclient.NexusClientImpl;
import deployment.mgmt.atrifacts.nexusclient.RepositoryPriorityServiceImpl;
import deployment.mgmt.atrifacts.strategies.classpathfile.ClasspathFileStrategy;
import deployment.mgmt.atrifacts.strategies.classpathfile.JarClasspathFileReaderImpl;
import deployment.mgmt.atrifacts.strategies.classpathfile.UnknownGroupResolverImpl;
import deployment.mgmt.configs.deploysettings.DeploySettingsImpl;
import deployment.mgmt.configs.deploysettings.SimpleEncryptionServiceImpl;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.filestructure.DeployFileStructureImpl;
import deployment.mgmt.configs.service.properties.MavenSettings;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.configs.service.properties.impl.PropertyServiceImpl;
import io.microconfig.configs.files.io.ConfigIoService;
import io.microconfig.configs.files.io.ConfigIoServiceSelector;
import io.microconfig.configs.files.io.properties.PropertiesConfigIoService;
import io.microconfig.configs.files.io.yaml.YamlConfigIoService;
import org.junit.jupiter.api.Disabled;

import java.io.File;
import java.util.List;

import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.util.Arrays.asList;

@Disabled
public class ClasspathTestIT {
    public static void main(String[] args) {
//        List<File> nexus = doResolve(new NexusClasspathStrategy());
        List<File> gradle = doResolve(gradleStrategy());
    }

    private static List<File> doResolve(ClasspathStrategy classpathStrategy) {
        DeployFileStructure fileStructure = DeployFileStructureImpl.init();

        String service = "cr-xls-export";
        MavenSettings mavenSettings = new PropertyServiceImpl(fileStructure, configIoSelector()).getProcessProperties(service).getMavenSettings();
        List<NexusRepository> nexusRepositories = mavenSettings.getNexusRepositories();
//        Artifact artifact = Artifact.fromMavenString("ru.sbt.cr.astreya.stresstest:stresstest-reports:RP-18.24-SNAPSHOT");
        Artifact artifact = Artifact.fromMavenString("ru.sbt.risk.tradehub:th-server:TH-18.24-SNAPSHOT");
//        Artifact artifact = Artifact.fromMavenString("ru.sbt.risk.ocp:ocp-core:OCP-18.23-SNAPSHOT");

        long t = System.currentTimeMillis();
        List<File> artifacts = classpathStrategy.downloadDependencies(artifact, false, nexusRepositories, mavenSettings.getLocalRepositoryDir(),
                fileStructure.logs().getMavenLogFile(service));
        announce("\n resolved " + artifacts.size() + " artifacts in " + secAfter(t));
        return artifacts;
    }

    private static ClasspathFileStrategy gradleStrategy() {
        NexusClient nexusClient = new NexusClientImpl(
                new RepositoryPriorityServiceImpl(asList("ru", "deployment")),
                new DeploySettingsImpl(DeployFileStructureImpl.init(), null, new SimpleEncryptionServiceImpl(), configIoSelector())
        );
        return new ClasspathFileStrategy(
                new JarClasspathFileReaderImpl(),
                new UnknownGroupResolverImpl(nexusClient),
                nexusClient
        );
    }

    private static ConfigIoService configIoSelector() {
        return new ConfigIoServiceSelector(new YamlConfigIoService(), new PropertiesConfigIoService());
    }
}