package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.configs.service.properties.ProcessProperties;

public interface ExtractService {
    void unzipArtifactIfNeeded(String service, ProcessProperties processProperties);
}
