package deployment.mgmt.process.start;

import deployment.mgmt.configs.service.properties.ProcessProperties;

public interface PreStartStep {
    void beforeStart(String service, ProcessProperties processProperties);
}
