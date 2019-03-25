package deployment.mgmt.process.runner;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.FilePermissionUtils.allowExecutionIfExists;
import static io.microconfig.utils.Logger.info;
import static mgmt.utils.ProcessUtil.startAndWait;

@RequiredArgsConstructor
public class ScriptRunnerImpl implements ScriptRunner {
    private final DeployFileStructure deployFileStructure;

    @Override
    public void runScript(String scriptName, String service) {
        if (scriptName == null) return;

        String scriptPath = getScriptFullPath(scriptName);
        info("Running script " + scriptPath + " for " + service);

        allowExecutionIfExists(new File(scriptPath));
        startAndWait(new ProcessBuilder(scriptPath)
                .directory(deployFileStructure.service().getServiceDir(service))
                .inheritIO()
        );
    }

    private String getScriptFullPath(String scriptName) {
        return scriptName.startsWith("/") ? scriptName : new File(deployFileStructure.configs().getScriptsDir(), scriptName).getAbsolutePath();
    }
}