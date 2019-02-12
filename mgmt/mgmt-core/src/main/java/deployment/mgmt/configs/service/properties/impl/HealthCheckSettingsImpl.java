package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.service.properties.HealthCheckSettings;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HealthCheckSettingsImpl implements HealthCheckSettings {
    private final ProcessProperties processProperties;

    @Override
    public String getSuccessMarker() {
        return processProperties.get("healthcheck.marker.success");
    }

    @Override
    public String getFailureMarker() {
        return processProperties.get("healthcheck.marker.failure");
    }
}
