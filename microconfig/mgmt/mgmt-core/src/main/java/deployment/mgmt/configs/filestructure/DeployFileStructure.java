package deployment.mgmt.configs.filestructure;

public interface DeployFileStructure {
    DeployDirs deploy();

    ConfigDirs configs();

    ServiceDirs service();

    ServiceLogDirs logs();

    ProcessDirs process();
}