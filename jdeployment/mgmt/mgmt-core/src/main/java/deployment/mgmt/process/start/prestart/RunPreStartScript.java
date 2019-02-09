package deployment.mgmt.process.start.prestart;

import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.runner.ScriptRunner;
import deployment.mgmt.process.start.PreStartStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RunPreStartScript implements PreStartStep {
    private final ScriptRunner scriptRunner;

    @Override
    public void beforeStart(String service, ProcessProperties processProperties) {
        scriptRunner.runScript(processProperties.getPrestartScriptName(), service);
    }
}