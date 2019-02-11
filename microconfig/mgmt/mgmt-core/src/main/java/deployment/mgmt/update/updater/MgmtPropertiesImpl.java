package deployment.mgmt.update.updater;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.componentgroup.GroupDescription;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl;
import io.microconfig.commands.factory.BuildCommands;
import io.microconfig.commands.factory.PropertyType;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static deployment.util.Logger.info;
import static io.microconfig.commands.factory.PropertyType.PROCESS;
import static io.microconfig.environments.Component.byType;
import static io.microconfig.properties.Property.withoutTempValues;

@RequiredArgsConstructor
public class MgmtPropertiesImpl implements MgmtProperties {
    private final DeployFileStructure deployFileStructure;
    private final ComponentGroupService componentGroupService;

    @Override //todo migrate  for DeploySettings::getMgmtNexusRepository
    public List<NexusRepository> resolveNexusRepositories() {
        BuildCommands buildCommands = initBuildCommands();
        PropertiesProvider propertiesProvider = buildCommands.newPropertiesProvider(PROCESS);
        EnvironmentProvider environmentProvider = buildCommands.getEnvironmentProvider();

        String serviceName = anyServiceFromCurrentGroup(environmentProvider);
        return resolveNexusUrlProperty(serviceName, propertiesProvider);
    }

    @Override
    public PropertiesProvider getPropertyProvider(PropertyType propertyType) {
        return initBuildCommands().newPropertiesProvider(propertyType);
    }

    @Override
    public EnvironmentProvider getEnvironmentProvider() {
        return initBuildCommands().getEnvironmentProvider();
    }

    private BuildCommands initBuildCommands() {
        return BuildCommands.init(deployFileStructure.configs().getInnerRepoDir(), deployFileStructure.service().getComponentsDir());
    }

    private List<NexusRepository> resolveNexusUrlProperty(String serviceName, PropertiesProvider propertiesProvider) {
        info("Resolving nexus repositories");
        Map<String, Property> properties = propertiesProvider.getProperties(byType(serviceName), componentGroupService.getEnv());
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("Can't resolver process properties for " + serviceName);
        }

        return new ProcessPropertiesImpl(withoutTempValues(properties), null).getMavenSettings().getNexusRepositories();
    }

    private String anyServiceFromCurrentGroup(EnvironmentProvider environmentProvider) {
        GroupDescription cg = componentGroupService.getDescription();

        return environmentProvider.getByName(cg.getEnv())
                .getComponentGroupByName(cg.getGroup())
                .getComponents()
                .get(0).getType();
    }
}