package deployment.mgmt.update.updater;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.componentgroup.GroupDescription;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl;
import io.microconfig.commands.build.factory.ConfigType;
import io.microconfig.commands.build.factory.MicroconfigFactory;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static io.microconfig.commands.build.factory.StandardConfigType.PROCESS;
import static io.microconfig.configs.Property.withoutTempValues;
import static io.microconfig.environments.Component.byType;
import static io.microconfig.utils.Logger.info;

@RequiredArgsConstructor
public class MgmtPropertiesImpl implements MgmtProperties {
    private final DeployFileStructure deployFileStructure;
    private final ComponentGroupService componentGroupService;

    @Override //todo migrate  for DeploySettings::getMgmtNexusRepository
    public List<NexusRepository> resolveNexusRepositories() {
        MicroconfigFactory microconfigFactory = initBuildCommands();
        ConfigProvider configProvider = microconfigFactory.newConfigProvider(PROCESS.type());
        EnvironmentProvider environmentProvider = microconfigFactory.getEnvironmentProvider();

        String serviceName = anyServiceFromCurrentGroup(environmentProvider);
        return resolveNexusUrlProperty(serviceName, configProvider);
    }

    @Override
    public ConfigProvider getConfigProvider(ConfigType configType) {
        return initBuildCommands().newConfigProvider(configType);
    }

    @Override
    public EnvironmentProvider getEnvironmentProvider() {
        return initBuildCommands().getEnvironmentProvider();
    }

    private MicroconfigFactory initBuildCommands() {
        return MicroconfigFactory.init(
                deployFileStructure.configs().getConfigSourcesRootDir(),
                deployFileStructure.service().getComponentsDir()
        );
    }

    private List<NexusRepository> resolveNexusUrlProperty(String serviceName, ConfigProvider configProvider) {
        info("Resolving nexus repositories");
        Map<String, Property> properties = configProvider.getProperties(byType(serviceName), componentGroupService.getEnv());
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