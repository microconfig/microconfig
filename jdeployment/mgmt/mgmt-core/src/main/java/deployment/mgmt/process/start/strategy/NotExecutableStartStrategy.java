package deployment.mgmt.process.start.strategy;

import deployment.mgmt.configs.service.metadata.MetadataProvider;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.StartHandle;
import deployment.mgmt.process.start.StartStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class NotExecutableStartStrategy implements StartStrategy {
    private final MetadataProvider metadataProvider;

    @Override
    public StartHandle createHandle(String service, String[] ignored, ProcessProperties processProperties, Map<String, String> ignored3) {
        return new StartHandle() {
            @Override
            public String getServiceName() {
                return service;
            }

            @Override
            public void executedCmdLine() {
                metadataProvider.updateLastRunStatus(service, true);
                metadataProvider.storePid(service, -1);
            }

            @Override
            public boolean awaitStartAndGetStatus() {
                return true;
            }

            @Override
            public ProcessProperties getProcessProperties() {
                return processProperties;
            }

            @Override
            public Exception getException() {
                return null;
            }
        };
    }

    @Override
    public boolean support(ProcessProperties processProperties) {
        return processProperties.isWebapp();
    }
}