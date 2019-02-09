package deployment.mgmt.process.runner;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.util.FileUtils.allowExecution;
import static deployment.util.Logger.info;
import static deployment.util.ProcessUtil.startAndWait;

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