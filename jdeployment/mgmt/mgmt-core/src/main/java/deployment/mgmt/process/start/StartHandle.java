package deployment.mgmt.process.start;

import deployment.mgmt.configs.service.properties.ProcessProperties;

public interface StartHandle {
    String getServiceName();

    void executedCmdLine();

    boolean awaitStartAndGetStatus();

    ProcessProperties getProcessProperties();

    Exception getException();
}