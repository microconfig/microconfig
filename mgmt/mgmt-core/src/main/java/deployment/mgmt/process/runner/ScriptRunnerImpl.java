package deployment.mgmt.process.runner;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.FilePermissionUtils.allowExecution;
import static io.microconfig.utils.Logger.info;
import static mgmt.utils.ProcessUtil.startAndWait;

@RequiredArgsConstructor
public class ScriptRunnerImpl implements ScriptRunner {
    private final DeployFileStructure deployFileStructure;

    @Override
    public void runScript(String scriptName, String service) {
        if (scriptName == null) return;

        String fullName = scriptName.startsWith("/") ? scriptName : new File(deployFileStructure.configs().getScriptsDir(), scriptName).getAbsolutePath();
        makeExecutable(new File(fullName));

        info("Running script " + fullName + " for " + service);

        startAndWait(new ProcessBuilder(fullName)
                .directory(deployFileStructure.service().getServiceDir(service))
                .inheritIO()
        );
    }

    private void makeExecutable(File script) {
        if (script.exists()) {
            allowExecution(script.toPath());
        }
    }
}