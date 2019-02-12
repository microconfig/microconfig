package deployment.mgmt.process.start.prestart;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.componentgroup.ServiceDescription;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.PreStartStep;
import lombok.RequiredArgsConstructor;

import static io.microconfig.utils.Logger.announce;

@RequiredArgsConstructor
public class ApplyAlteredVersion implements PreStartStep {
    private final ComponentGroupService componentGroupService;

    @Override
    public void beforeStart(String service, ProcessProperties processProperties) {
        componentGroupService.getAlteredVersionService(service)
                .map(ServiceDescription::getVersion)
                .filter(version -> !version.equals(processProperties.getVersion()))
                .ifPresent(version -> {
                    announce("Overriding " + service + " config version to " + version);
                    processProperties.changeVersion(version);
                });
    }
}