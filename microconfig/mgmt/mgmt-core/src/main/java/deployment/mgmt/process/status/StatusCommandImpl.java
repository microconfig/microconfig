package deployment.mgmt.process.status;

import deployment.mgmt.configs.componentgroup.ServiceDescription;
import deployment.mgmt.configs.service.metadata.MetadataProvider;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import deployment.util.Logger;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static deployment.mgmt.process.status.ExecutionStatus.*;
import static deployment.mgmt.process.status.ServiceType.*;
import static deployment.util.ProcessUtil.belongsToCurrentUser;
import static java.util.stream.Stream.of;


@RequiredArgsConstructor
public class StatusCommandImpl implements StatusCommand {
    private final MetadataProvider metadataProvider;
    private final PropertyService propertyService;

    @Override
    public void printStatus(String... services) {
        of(services).map(this::getStatus).forEach(s -> s.output(Logger::info));
    }

    @Override
    public ServiceStatus getStatus(String service) {
        if (!propertyService.serviceExists(service)) {
            return ServiceStatus.unknown(service);
        }

        ProcessProperties processProperties = propertyService.getProcessProperties(service);
        ServiceDescription sd = new ServiceDescription(service, processProperties.getVersion());
        String configVersion = processProperties.getConfigVersion();
        ServiceType serviceType = processProperties.isPatcher() ? PATCHER : (processProperties.isWebapp() || processProperties.isTask()) ? TASK : SERVICE;

        if (!metadataProvider.isLastRunSucceed(sd.getName()))
            return new ServiceStatus(sd, configVersion, serviceType, FAILED);
        Optional<Long> pid = metadataProvider.lastPid(sd.getName());
        if (!pid.isPresent()) {
            return new ServiceStatus(sd, configVersion, serviceType, serviceType == PATCHER ? NOT_EXECUTED : STOPPED);
        }
        Optional<ProcessHandle> processHandle = pid.flatMap(ProcessHandle::of);
        if (belongsToCurrentUser(processHandle)) {
            return ServiceStatus.fromProcess(sd, configVersion, serviceType, RUNNING, processHandle.orElseThrow());
        }

        return new ServiceStatus(sd, configVersion, serviceType, serviceType == PATCHER ? EXECUTED : STOPPED);
    }
}