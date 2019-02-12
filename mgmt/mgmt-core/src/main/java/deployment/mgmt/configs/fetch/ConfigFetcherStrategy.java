package deployment.mgmt.configs.fetch;

import java.io.File;
import java.util.List;

public interface ConfigFetcherStrategy {
    void fetchConfigs(String configVersion, File destination);

    List<String> newConfigReleases(String service, String currentConfigVersion, boolean includeCurrentVersion);
}
