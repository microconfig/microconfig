package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType;
import io.microconfig.utils.StreamUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static deployment.mgmt.atrifacts.Artifact.fromMavenString;
import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Stream.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RepositoryPriorityServiceImplTest {
    private final RepositoryPriorityService priorityService = new RepositoryPriorityServiceImpl(List.of("ru"));
    private final List<NexusRepository> repositories = of(RepositoryType.values()).map(this::repository).collect(toUnmodifiableList());

    @Test
    public void withPriority() {
        test(fromMavenString("ru:a:b"), RELEASE, DEPENDENCIES, THIRDPARTY);
        test(fromMavenString("ru:ru:1.3"), RELEASE, DEPENDENCIES, THIRDPARTY);
        test(fromMavenString("ru:ru:3-SNAPSHOT"), SNAPSHOTS, DEPENDENCIES, THIRDPARTY);
        test(fromMavenString("a:b:1-SNAPSHOT"), SNAPSHOTS, DEPENDENCIES, THIRDPARTY);
        test(fromMavenString("com.ru:ru:4"), DEPENDENCIES, THIRDPARTY);
    }

    private void test(Artifact artifact, RepositoryType... first) {
        List<NexusRepository> result = priorityService.withPriority(repositories, artifact);
        assertEquals(repositories.size(), result.size());
        assertEquals(asList(first), StreamUtils.map(result.subList(0, first.length), NexusRepository::getRepositoryType));
    }

    private NexusRepository repository(RepositoryType repositoryType) {
        return new NexusRepository(repositoryType.name(), "http://" + repositoryType, repositoryType);
    }
}