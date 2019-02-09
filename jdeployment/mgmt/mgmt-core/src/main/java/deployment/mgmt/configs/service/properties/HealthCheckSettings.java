package deployment.mgmt.configs.service.properties;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public interface HealthCheckSettings {
    String getSuccessMarker();

    String getFailureMarker();

    default Set<String> getLogMarkers() {
        return Stream.of(getSuccessMarker(), getFailureMarker())
                .filter(Objects::nonNull)
                .collect(toSet());
    }
}
