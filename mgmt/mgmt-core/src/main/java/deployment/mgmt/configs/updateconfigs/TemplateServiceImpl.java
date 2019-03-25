package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.PropertyService;
import io.microconfig.commands.buildconfig.factory.MicroconfigFactory;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.features.templates.CopyTemplatesService;
import lombok.RequiredArgsConstructor;

import static io.microconfig.commands.buildconfig.factory.StandardConfigType.SERVICE;
import static io.microconfig.environments.Component.byType;
import static java.lang.ThreadLocal.withInitial;

@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final CopyTemplatesService copyTemplatesService;

    private final ComponentGroupService componentGroupService;
    private final DeployFileStructure deployFileStructure;
    private final PropertyService propertyService;

    private final ThreadLocal<PropertyResolver> resolver = withInitial(this::newPropertyResolver);

    @Override
    public void copyTemplates(String service) {
        copyTemplatesService.copyTemplates(
                new EnvComponent(byType(service), componentGroupService.getEnv()),
                deployFileStructure.service().getServiceDir(service),
                propertyService.getServiceProperties(service),
                resolver.get()
        );
    }

    private PropertyResolver newPropertyResolver() {
        MicroconfigFactory factory = MicroconfigFactory.init(
                deployFileStructure.configs().getMicroconfigSourcesRootDir(),
                deployFileStructure.service().getComponentsDir()
        );
        return factory.newResolver(factory.newConfigProvider(SERVICE.getConfigType()));
    }
}
