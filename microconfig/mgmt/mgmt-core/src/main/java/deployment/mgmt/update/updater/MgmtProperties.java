package deployment.mgmt.update.updater;

import io.microconfig.command.factory.PropertyType;
import io.microconfig.environment.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.util.List;

public interface MgmtProperties {
    List<NexusRepository> resolveNexusRepositories();

    PropertiesProvider getPropertyProvider(PropertyType propertyType);

    EnvironmentProvider getEnvironmentProvider();
}