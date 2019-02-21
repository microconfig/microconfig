package deployment.mgmt.update.updater;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.componentgroup.GroupDescription;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl;
import io.microconfig.commands.factory.MicroconfigFactory;
import io.microconfig.commands.factory.ConfigType;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static io.microconfig.commands.factory.ConfigType.PROCESS;
import static io.microconfig.environments.Component.byType;
import static io.microconfig.properties.Property.withoutTempValues;
import static io.microconfig.utils.Logger.info;

@RequiredArgsConstructor
public class MgmtPropertiesImpl implements MgmtProperties {
    private final DeployFileStructure deployFileStructure;
    private final ComponentGroupService componentGroupService;

    @Override //todo migrate  for DeploySettings::getMgmtNexusRepository
    public List<NexusRepository> resolveNexusRepositories() {
        MicroconfigFactory microconfigFactory = initBuildCommands();
        PropertiesProvider propertiesProvider = microconfigFactory.newPropertiesProvider(PROCESS);
        EnvironmentProvider environmentProvider = microconfigFactory.getEnvironmentProvider();

        String serviceName = anyServiceFromCurrentGroup(environmentProvider);
        return resolveNexusUrlProperty(serviceName, propertiesProvider);
    }

    @Override
    public PropertiesProvider getPropertyProvider(ConfigType configType) {
        return initBuildCommands().newPropertiesProvider(configType);
    }

    @Override
    public EnvironmentProvider getEnvironmentProvider() {
        return initBuildCommands().getEnvironmentProvider();
    }

    private MicroconfigFactory initBuildCommands() {
        return MicroconfigFactory.init(
                deployFileStructure.configs().getInnerRepoDir(),
                deployFileStructure.service().getComponentsDir()
        );
    }

    private List<NexusRepository> resolveNexusUrlProperty(String serviceName, PropertiesProvider propertiesProvider) {
        info("Resolving nexus repositories");
        Map<String, Property> properties = propertiesProvider.getProperties(byType(serviceName), componentGroupService.getEnv());
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("Can't resolve process properties for " + serviceName);
        }

        return ProcessPropertiesImpl.fromMap(withoutTempValues(properties)).getMavenSettings().getNexusRepositories();
    }

    private String anyServiceFromCurrentGroup(EnvironmentProvider environmentProvider) {
        GroupDescription cg = componentGroupService.getDescription();

        return environmentProvider.getByName(cg.getEnv())
                .getGroupByName(cg.getGroup())
                .getComponents()
                .get(0).getType();
    }
}