package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import io.microconfig.io.ConfigIoService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl.fromFile;
import static deployment.mgmt.utils.EnvResolver.resolveEnvVariable;
import static java.util.Collections.unmodifiableMap;


@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final DeployFileStructure deployFileStructure;
    private final ConfigIoService configIoService;

    @Override
    public boolean serviceExists(String service) {
        return deployFileStructure.service().getServiceDir(service).exists() && deployFileStructure.process().getProcessPropertiesFile(service).exists();
    }

    @Override
    public Map<String, String> getServiceProperties(String service) {
        return unmodifiableMap(configIoService.read(deployFileStructure.service().getServicePropertiesFile(service)));
    }

    @Override
    public ProcessProperties getProcessProperties(String service) {
        return fromFile(deployFileStructure.process().getProcessPropertiesFile(service), configIoService);
    }

    @Override
    public Map<String, String> getEnvVariables(String service) {
        return resolveEnvVariable(configIoService.read(deployFileStructure.process().getEnvVariablesFile(service)));
    }
}