package deployment.mgmt.stat.releases;

import java.util.List;

public interface ReadyReleasesService {
    ReadyReleases currentReleaseToServices(List<String> services);

    ReadyReleases serviceToNewReleases(List<String> services);

    ReadyReleases newReleaseToServices(List<String> services, ReleaseType releaseType);
}