package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.filestructure.ProcessDirs;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import deployment.mgmt.process.runner.ScriptRunner;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static io.microconfig.utils.FileUtils.copy;
import static io.microconfig.utils.Logger.error;

@RequiredArgsConstructor
public class NewServicePreparerImpl implements NewServicePreparer {
    private final ClasspathService classpathService;
    private final DeployFileStructure deployFileStructure;
    private final PropertyService propertyService;
    private final ScriptRunner scriptRunner;

    @Override
    public void prepare(List<String> services, boolean skipClasspathBuildForSnapshot) {
        services.forEach(s -> doPrepare(s, skipClasspathBuildForSnapshot));
    }

    private void doPrepare(String service, boolean skipClasspathBuildForSnapshot) {
        try {
            ProcessProperties processProperties = propertyService.getProcessProperties(service);

            copyLogConfigToServiceDir(service);
            buildClasspath(service, processProperties, skipClasspathBuildForSnapshot);
            runPrepareDirScript(service, processProperties);
        } catch (RuntimeException e) {
            error("Can't prepare " + service, e);
        }
    }

    private void copyLogConfigToServiceDir(String service) {
        Consumer<File> copyToParent = source -> {
            File serviceRoot = source.getParentFile().getParentFile();
            File logConfigDestination = new File(serviceRoot, source.getName());
            copy(source, logConfigDestination);
        };

        ProcessDirs processDirs = deployFileStructure.process();
        copyToParent.accept(processDirs.getProcessFile(service, "log4j.properties"));
        copyToParent.accept(processDirs.getProcessFile(service, "log4j2.properties"));
    }

    private void buildClasspath(String service, ProcessProperties processProperties,
                                boolean skipClasspathBuildForSnapshot) {
        classpathService.classpathFor(service)
                .skipIfSnapshot(skipClasspathBuildForSnapshot)
                .buildUsing(processProperties);
    }

    private void runPrepareDirScript(String service, ProcessProperties processProperties) {
        scriptRunner.runScript(processProperties.getPrepareDirScriptName(), service);
    }
}