package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.NexusRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.*;
import static deployment.mgmt.configs.service.properties.NexusRepository.sortWithPriority;

@RequiredArgsConstructor
public class RepositoryPriorityServiceImpl implements RepositoryPriorityService {
    private final List<String> projectRootPackages;

    @Override
    public List<NexusRepository> withPriority(List<NexusRepository> repositories, Artifact artifact) {
        boolean snapshot = artifact.isSnapshot();

        if (!snapshot && hasRootGroupId(artifact.getGroupId())) {
            return sortWithPriority(repositories, RELEASE, DEPENDENCIES, THIRDPARTY);
        }

        if (snapshot) {
            return sortWithPriority(repositories, SNAPSHOTS, DEPENDENCIES, THIRDPARTY);
        }

        if (artifact.isArchive()) {
            return sortWithPriority(repositories, THIRDPARTY, DEPENDENCIES, RELEASE);
        }

        return sortWithPriority(repositories, DEPENDENCIES, THIRDPARTY, RELEASE);
    }

    private boolean hasRootGroupId(String groupId) {
        return projectRootPackages.stream().anyMatch(groupId::startsWith);
    }
}