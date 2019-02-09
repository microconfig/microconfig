package deployment.mgmt.api.json;

import deployment.console.Hidden;

public interface MgmtJsonApi {
    @Hidden
    void memoryUsageJson();

    @Hidden
    void envMemoryUsageJson();

    @Hidden
    void currentReleaseToServicesJson();

    @Hidden
    void newReleaseToServicesJson(String type);
}