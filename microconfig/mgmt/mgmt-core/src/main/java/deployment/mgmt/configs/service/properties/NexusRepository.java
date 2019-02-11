package deployment.mgmt.configs.service.properties;

import lombok.Getter;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType.RELEASE;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toUnmodifiableList;

@Getter
public class NexusRepository {
    private final String name;
    private final String url;
    private final String repositoryName;
    private final RepositoryType repositoryType;

    public NexusRepository(String name, String url, RepositoryType repositoryType) {
        this.name = name;
        this.repositoryType = repositoryType;

        UnaryOperator<String> withoutLastSlash = v -> v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
        this.url = withoutLastSlash.apply(url.replace("\\", ""));
        String repo = "/content/repositories/";
        this.repositoryName = url.contains(repo) ? this.url.substring(url.lastIndexOf(repo) + repo.length()) : null;
    }

    public static List<NexusRepository> sortWithPriority(List<NexusRepository> repositories, RepositoryType... priorities) {
        List<RepositoryType> index = asList(priorities);

        return repositories.stream()
                .sorted(comparing(r -> {
                    int i = index.indexOf(r.getRepositoryType());
                    return i < 0 ? repositories.size() : i;
                }))
                .collect(toUnmodifiableList());
    }

    public static NexusRepository releaseRepo(List<NexusRepository> nexusRepositories) {
        return nexusRepositories.stream()
                .filter(r -> r.repositoryType == RELEASE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find release repo from " + nexusRepositories));
    }

    public String getNexusBaseUrl() {
        return url.substring(0, url.indexOf("/content/"));
    }

    public enum RepositoryType {
        RELEASE,
        SNAPSHOTS,
        DEPENDENCIES,
        THIRDPARTY;

        public static RepositoryType typeOf(String key) {
            return Stream.of(values())
                    .filter(t -> key.contains(t.name().toLowerCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported repository type " + key));
        }
    }

    @Override
    public String toString() {
        return url;
    }
}