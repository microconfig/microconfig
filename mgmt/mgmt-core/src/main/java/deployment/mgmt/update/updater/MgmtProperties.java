package deployment.mgmt.update.updater;

import deployment.mgmt.configs.service.properties.NexusRepository;
import io.microconfig.entry.factory.ConfigType;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.environments.EnvironmentProvider;

import java.util.List;

public interface MgmtProperties {
    List<NexusRepository> resolveNexusRepositories();

    ConfigProvider getConfigProvider(ConfigType configType);

    EnvironmentProvider getEnvironmentProvider();
}