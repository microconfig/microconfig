package deployment.dashboard.release;

import deployment.dashboard.shared.mgmt.MgmtCall;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.naturalOrder;

@Service
@RequiredArgsConstructor
public class ReleaseServiceImpl implements ReleaseService {
    private final MgmtCall mgmtCall;

    @Override
    public String currentRelease(String group) {
        return mgmtCall.executeRemotely(group, "currentReleaseToServicesJson");
    }

    @Override
    public String newReleases(String group) {
        return mgmtCall.executeRemotely(group, "newReleaseToServicesJson config");
    }

    @Override
    public String approve(String group, Map<String, List<String>> releaseToServices) {
        return "Approved " + lastVersion(releaseToServices);
    }

    @Override
    public void deploy(String group, Map<String, List<String>> releaseToServices, OutputStream logDestination) {
        String maxRelease = lastVersion(releaseToServices); //todo
        String postfix = maxRelease.substring(maxRelease.lastIndexOf('.'));

        mgmtCall.executeRemotely(group, "smartDeploy " + maxRelease + " " + postfix, logDestination);
    }

    @Override
    public void healthcheck(String group, OutputStream outputStream) {
        mgmtCall.executeRemotely(group, "healthcheck", outputStream);
    }

    @Override
    public void status(String group, OutputStream outputStream) {
        mgmtCall.executeRemotely(group, "status", outputStream);
    }

    @Override
    public String releaseHistory(String group) {
        return "history";
    }

    private String lastVersion(Map<String, List<String>> releaseToServices) {
        return releaseToServices.keySet()
                .stream()
                .max(naturalOrder())
                .orElseThrow();
    }
}