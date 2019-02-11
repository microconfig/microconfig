package deployment.mgmt.update.updater;

import deployment.mgmt.configs.service.properties.NexusRepository;
import io.microconfig.commands.factory.PropertyType;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;

import java.util.List;

public interface MgmtProperties {
    List<NexusRepository> resolveNexusRepositories();

    PropertiesProvider getPropertyProvider(PropertyType propertyType);

    EnvironmentProvider getEnvironmentProvider();
}