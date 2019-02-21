package deployment.mgmt.update.updater;

import deployment.mgmt.configs.service.properties.NexusRepository;
import io.microconfig.commands.factory.ConfigType;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;

import java.util.List;

public interface MgmtProperties {
    List<NexusRepository> resolveNexusRepositories();

    PropertiesProvider getPropertyProvider(ConfigType configType);

    EnvironmentProvider getEnvironmentProvider();
}