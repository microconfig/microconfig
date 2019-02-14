package deployment.mgmt.process.start.strategy;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.metadata.MetadataProvider;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.StartHandle;
import deployment.mgmt.process.start.StartHandleImpl;
import deployment.mgmt.process.start.StartStrategy;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.utils.FilePermissionUtils.allowExecution;
import static io.microconfig.utils.FileUtils.write;

@RequiredArgsConstructor
public class AppStartStrategy implements StartStrategy {
    private final DeployFileStructure deployFileStructure;
    private final MetadataProvider metadataProvider;

    @Override
    public StartHandle createHandle(String service, String[] ignored, ProcessProperties processProperties, Map<String, String> envVariables) {
        String processStartCommand = processProperties.getNotJavaAppSettings().getProcessStartCommand();

        File cmdFile = deployFileStructure.process().getLastCmdLineFile(service);
        write(cmdFile, processStartCommand);
        allowExecution(cmdFile.toPath()); //todo2 it creates child process with another pid

        ProcessBuilder processBuilder = new ProcessBuilder(cmdFile.getAbsolutePath())
                .directory(deployFileStructure.service().getServiceDir(service))
                .redirectOutput(deployFileStructure.logs().getLogFile(service, processProperties.getLogFileName(service)))
                .redirectErrorStream(true);

        processBuilder.environment().putAll(envVariables);

        return new StartHandleImpl(service, processBuilder, processProperties, deployFileStructure, metadataProvider);
    }

    @Override
    public boolean support(ProcessProperties processProperties) {
        return !processProperties.isJavaApp();
    }
}