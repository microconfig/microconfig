package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.service.properties.NotJavaAppSettings;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotJavaAppSettingsImpl implements NotJavaAppSettings {
    private final ProcessProperties processProperties;

    @Override
    public String getProcessStartCommand() {
        return (processExec() + " " + processArgs()).trim();
    }

    private String processExec() {
        return processProperties.get("process.exec");
    }

    private String processArgs() {
        return processProperties.getOrDefault("process.args", "");
    }
}
