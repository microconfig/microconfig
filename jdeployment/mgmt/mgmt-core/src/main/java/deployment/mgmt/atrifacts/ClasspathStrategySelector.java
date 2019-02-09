package deployment.mgmt.atrifacts;

import deployment.mgmt.configs.service.properties.MavenSettings;

interface ClasspathStrategySelector {
    ClasspathStrategy selectStrategy(String service, MavenSettings mavenSettings);
}