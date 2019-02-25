package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import io.microconfig.configs.files.io.ConfigIoService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl.fromFile;
import static deployment.mgmt.utils.EnvResolver.resolveEnvVariable;


@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final DeployFileStructure deployFileStructure;
    private final ConfigIoService configIo;

    @Override
    public boolean serviceExists(String service) {
        return deployFileStructure.service().getServiceDir(service).exists() && deployFileStructure.process().getProcessPropertiesFile(service).exists();
    }

    @Override
    public Map<String, String> getServiceProperties(String service) {
        return configIo.read(deployFileStructure.service().getServicePropertiesFile(service)).propertiesAsMap();
    }

    @Override
    public ProcessProperties getProcessProperties(String service) {
        return fromFile(deployFileStructure.process().getProcessPropertiesFile(service), configIo);
    }

    @Override
    public Map<String, String> getEnvVariables(String service) {
        File file = deployFileStructure.process().getEnvVariablesFile(service);
        Map<String, String> properties = configIo.read(file).propertiesAsMap();
        return resolveEnvVariable(properties);
    }
}