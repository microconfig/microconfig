package deployment.mgmt.configs.filestructure;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeployFileStructureImpl implements DeployFileStructure {
    private final DeployDirs deployDirs;
    private final ConfigDirs configDirs;
    private final ServiceDirs serviceDirs;
    private final ServiceLogDirs serviceLogDirs;
    private final ProcessDirs processDirs;

    public static DeployFileStructure init() {
        ServiceDirs serviceDirs = new ServiceDirsImpl();
        return new DeployFileStructureImpl(new DeployDirsImpl(), new ConfigDirsImpl(), serviceDirs, new ServiceLogDirsImpl(serviceDirs), new ProcessDirsImpl(serviceDirs));
    }

    @Override
    public DeployDirs deploy() {
        return deployDirs;
    }

    @Override
    public ConfigDirs configs() {
        return configDirs;
    }

    @Override
    public ServiceDirs service() {
        return serviceDirs;
    }

    @Override
    public ServiceLogDirs logs() {
        return serviceLogDirs;
    }

    @Override
    public ProcessDirs process() {
        return processDirs;
    }
}
