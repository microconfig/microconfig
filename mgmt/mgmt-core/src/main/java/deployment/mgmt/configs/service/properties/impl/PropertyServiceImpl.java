package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static deployment.mgmt.configs.service.properties.impl.ProcessPropertiesImpl.fromFile;
import static deployment.util.OsUtil.resolveEnvVariable;
import static deployment.util.PropertiesUtils.readProperties;
import static java.util.Collections.unmodifiableMap;


@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {
    private final DeployFileStructure deployFileStructure;

    @Override
    public boolean serviceExists(String service) {
        return deployFileStructure.service().getServiceDir(service).exists() && deployFileStructure.process().getProcessPropertiesFile(service).exists();
    }

    @Override
    public Map<String, String> getServiceProperties(String service) {
        return unmodifiableMap(readProperties(deployFileStructure.service().getServicePropertiesFile(service)));
    }

    @Override
    public ProcessProperties getProcessProperties(String service) {
        return fromFile(deployFileStructure.process().getProcessPropertiesFile(service));
    }

    @Override
    public Map<String, String> getEnvVariables(String service) {
        return resolveEnvVariable(readProperties(deployFileStructure.process().getEnvVariablesFile(service)));
    }
}