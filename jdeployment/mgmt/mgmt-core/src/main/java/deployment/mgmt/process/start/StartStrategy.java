package deployment.mgmt.process.start;

import deployment.mgmt.configs.service.properties.ProcessProperties;

import java.util.Map;

public interface StartStrategy {
    StartHandle createHandle(String service, String[] args, ProcessProperties processProperties, Map<String, String> envVariables);

    boolean support(ProcessProperties processProperties);
}
