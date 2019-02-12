package deployment.mgmt.configs.fetch;

import java.util.List;

public interface ConfigFetcher {
    void fetchConfigs(String configVersion);

    List<String> newConfigReleases(String service, boolean includeCurrentVersion);
}
