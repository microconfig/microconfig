package deployment.mgmt.configs.service.properties;

import java.util.Map;

public interface PropertyService {
    boolean serviceExists(String service);

    ProcessProperties getProcessProperties(String service);

    Map<String, String> getServiceProperties(String service);

    Map<String, String> getEnvVariables(String service);
}
