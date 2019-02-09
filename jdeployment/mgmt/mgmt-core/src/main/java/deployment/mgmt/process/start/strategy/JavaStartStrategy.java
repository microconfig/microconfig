package deployment.mgmt.process.start.strategy;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.metadata.MetadataProvider;
import deployment.mgmt.configs.service.properties.JavaAppSettings;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.StartHandle;
import deployment.mgmt.process.start.StartHandleImpl;
import deployment.mgmt.process.start.StartStrategy;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static deployment.util.FileUtils.write;
import static java.lang.String.join;
import static java.util.Collections.addAll;

@RequiredArgsConstructor
public class JavaStartStrategy implements StartStrategy {
    private static final String CLASSPATH = "CLASSPATH";

    private final DeployFileStructure deployFileStructure;
    private final MetadataProvider metadataProvider;
    private final ClasspathService classpathService;

    @Override
    public StartHandle createHandle(String service, String[] args, ProcessProperties processProperties, Map<String, String> envVariables) {
        ProcessBuilder processBuilder = prepareProcess(service, args, processProperties, envVariables);
        storeCmdLine(service, join(" ", processBuilder.command()));

        return new StartHandleImpl(service, processBuilder, processProperties, deployFileStructure, metadataProvider);
    }

    private ProcessBuilder prepareProcess(String service, String[] args, ProcessProperties processProperties, Map<String, String> envVariables) {
        ProcessBuilder processBuilder = new ProcessBuilder().command(getCommand(processProperties, args))
                .directory(deployFileStructure.service().getServiceDir(service))
                .redirectOutput(deployFileStructure.logs().getLogFile(service, "out.log"))
                .redirectErrorStream(true);

//        if (processProperties.inheritIO()) {
//            processBuilder.inheritIO(); //todo doesn't write logs
//        }

        processBuilder.environment().putAll(envVariables);
        processBuilder.environment().put(CLASSPATH, classpathService.classpathFor(service).current().asString());

        return processBuilder;
    }

    private List<String> getCommand(ProcessProperties props, String[] args) {
        JavaAppSettings javaProps = props.getJavaAppSettings();

        List<String> command = new ArrayList<>();
        command.add(javaProps.getJavaPath());
        command.addAll(javaProps.getJavaOptsAsList());
        command.add(javaProps.getMainClass());
        command.addAll(javaProps.getProcessArgsAsList());
        if (args != null) {
            addAll(command, args);
        }

        return command;
    }

    private void storeCmdLine(String service, String cmd) {
        write(deployFileStructure.process().getLastCmdLineFile(service), cmd);
    }

    @Override
    public boolean support(ProcessProperties processProperties) {
        return processProperties.isJavaApp();
    }
}