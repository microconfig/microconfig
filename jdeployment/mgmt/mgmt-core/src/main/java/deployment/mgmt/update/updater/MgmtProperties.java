package deployment.mgmt.update.updater;

import deployment.configs.command.factory.PropertyType;
import deployment.configs.environment.EnvironmentProvider;
import deployment.configs.properties.PropertiesProvider;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.util.List;

public interface MgmtProperties {
    List<NexusRepository> resolveNexusRepositories();

    PropertiesProvider getPropertyProvider(PropertyType propertyType);

    EnvironmentProvider getEnvironmentProvider();
}