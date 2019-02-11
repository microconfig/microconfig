package deployment.mgmt.update.updater;

import io.microconfig.configs.command.factory.PropertyType;
import io.microconfig.configs.environment.EnvironmentProvider;
import io.microconfig.configs.properties.PropertiesProvider;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.util.List;

public interface MgmtProperties {
    List<NexusRepository> resolveNexusRepositories();

    PropertiesProvider getPropertyProvider(PropertyType propertyType);

    EnvironmentProvider getEnvironmentProvider();
}