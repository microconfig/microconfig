package deployment.mgmt.configs.fetch.strategy;

import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.fetch.ConfigFetcherStrategy;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.version.Version;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

import static io.microconfig.utils.FileUtils.userHome;
import static io.microconfig.utils.Logger.announce;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static mgmt.utils.ProcessUtil.executeAndReadOutput;
import static mgmt.utils.ProcessUtil.startAndWait;

@RequiredArgsConstructor
public class GitConfigStrategy implements ConfigFetcherStrategy {
    private final DeploySettings deploySettings;
    private final DeployFileStructure deployFileStructure;

    private List<String> tags;

    @Override
    public void fetchConfigs(String gitTagOrBranch, File destination) {
        String gitUrl = deploySettings.getConfigGitUrl();
        announce("Cloning " + urlWithoutPassword(gitUrl) + " [" + gitTagOrBranch + "] to " + destination);

        int status = startAndWait(
                new ProcessBuilder("git", "clone", "-b", gitTagOrBranch, "--single-branch", "--depth", "1", gitUrl, destination.getAbsolutePath())
                        .directory(userHome())
                        .inheritIO()
        );

        if (status != 0) {
            throw new IllegalArgumentException("Git repo clone error");
        }
    }

    @Override
    public List<String> newConfigReleases(String service, String currentConfigVersion, boolean includeCurrentVersion) {
        return new Version(currentConfigVersion).filterNewReleases(getAllTags().stream(), includeCurrentVersion, 5);
    }

    private synchronized List<String> getAllTags() {
        List<String> tags = this.tags;
        if (tags == null) {
            tags = this.tags = fetchTags();
        }
        return tags;
    }

    private List<String> fetchTags() {
        String output = executeAndReadOutput(
                new ProcessBuilder("git", "ls-remote", "--tags")
                        .directory(deployFileStructure.configs().getConfigsRootDir())
        );
        return of(output.split("\n"))
                .map(s -> s.split("\\s+")[1])
                .map(s -> s.substring(s.lastIndexOf('/') + 1))
                .collect(toList());
    }

    private String urlWithoutPassword(String gitUrl) {
        return gitUrl.substring(gitUrl.indexOf('@') + 1);
    }
}