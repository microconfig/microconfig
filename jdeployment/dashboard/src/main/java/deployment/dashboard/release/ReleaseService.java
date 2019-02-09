package deployment.dashboard.release;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ReleaseService {
    String currentRelease(String group);

    String newReleases(String group);

    String approve(String group, Map<String, List<String>> releaseToServices);

    void deploy(String group, Map<String, List<String>> releaseToServices, OutputStream logDestination);

    void healthcheck(String group, OutputStream outputStream);

    void status(String group, OutputStream outputStream);

    String releaseHistory(String group);
}
