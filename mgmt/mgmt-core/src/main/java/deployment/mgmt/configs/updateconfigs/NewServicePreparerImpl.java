package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.filestructure.ProcessDirs;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import deployment.mgmt.process.runner.ScriptRunner;
import deployment.mgmt.utils.ZipUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static deployment.mgmt.utils.ZipUtils.unzip;
import static io.microconfig.utils.FileUtils.copy;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.SystemPropertiesUtils.hasSystemFlag;

@RequiredArgsConstructor
public class NewServicePreparerImpl implements NewServicePreparer {
    private final PropertyService propertyService;
    private final DeployFileStructure deployFileStructure;
    private final ClasspathService classpathService;
    private final ExtractService extractService;
    private final ScriptRunner scriptRunner;
    private final TemplateService templateService;

    @Override
    public void prepare(List<String> services, boolean skipClasspathBuildForSnapshot) {
        services.forEach(service -> doPrepare(service, skipClasspathBuildForSnapshot));
    }

    private void doPrepare(String service, boolean skipClasspathBuildForSnapshot) {
        try {
            ProcessProperties processProperties = propertyService.getProcessProperties(service);

            copyLogDescriptorsToServiceDir(service);
            buildClasspath(service, processProperties, skipClasspathBuildForSnapshot);
            unzipArtifactIfNeeded(service, processProperties);
            runPrepareDirScript(service, processProperties);
            copyTemplates(service);
        } catch (RuntimeException e) {
            error("Can't prepare " + service, e);
        }
    }

    private void copyLogDescriptorsToServiceDir(String service) {
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

    private void unzipArtifactIfNeeded(String service, ProcessProperties processProperties) {
        extractService.unzipArtifactIfNeeded(service, processProperties);
    }
    private void runPrepareDirScript(String service, ProcessProperties processProperties) {
        if (hasSystemFlag("skipScripts")) return;
        scriptRunner.runScript(processProperties.getPrepareDirScriptName(), service);
    }

    private void copyTemplates(String service) {
        templateService.copyTemplates(service);
    }
}