package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.NexusRepository;

import java.util.List;

interface RepositoryPriorityService {
    List<NexusRepository> withPriority(List<NexusRepository> repositories, Artifact artifact);
}
