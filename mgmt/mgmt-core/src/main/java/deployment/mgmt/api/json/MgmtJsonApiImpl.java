package deployment.mgmt.api.json;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.stat.monitoring.MonitoringService;
import deployment.mgmt.stat.releases.ReadyReleasesService;
import deployment.mgmt.stat.releases.ReleaseType;
import io.microconfig.utils.Logger;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MgmtJsonApiImpl implements MgmtJsonApi {
    private final MonitoringService monitoringService;
    private final ReadyReleasesService readyReleasesService;
    private final ComponentGroupService componentGroupService;

    @Override
    public void envMemoryUsageJson() {
        monitoringService.getEnvMemoryUsage()
                .outputAsJsonTo(Logger::info, true);
    }

    @Override
    public void memoryUsageJson() {
        monitoringService.getGroupMemoryUsage()
                .outputAsJsonTo(Logger::info, false);
    }

    @Override
    public void currentReleaseToServicesJson() {
        readyReleasesService.currentReleaseToServices(componentGroupService.getServices())
                .outputAsJsonTo(Logger::info);
    }

    @Override
    public void newReleaseToServicesJson(String type) {
        readyReleasesService.newReleaseToServices(componentGroupService.getServices(), ReleaseType.valueOf(type.toUpperCase()))
                .outputAsJsonTo(Logger::info);
    }
}