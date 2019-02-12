package deployment.mgmt.configs.service.properties.impl;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.configs.service.properties.ClasspathStrategyType;
import deployment.mgmt.configs.service.properties.MavenSettings;
import deployment.mgmt.configs.service.properties.NexusRepository;
import deployment.mgmt.configs.service.properties.NexusRepository.RepositoryType;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static deployment.mgmt.atrifacts.Artifact.LOCAL_REPO_DIR;
import static deployment.mgmt.configs.service.properties.ClasspathStrategyType.NEXUS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class MavenSettingsImpl implements MavenSettings {
    private static final String MAVEN_VERSION_KEY = "maven.version";

    private final ProcessProperties processProperties;

    @Override
    public Artifact getArtifact() {
        return Artifact.fromMavenString(processProperties.get(MAVEN_VERSION_KEY));
    }

    @Override
    public boolean resolveSingleArtifact() {
        return processProperties.hasTrueValue("maven.resolve.single.artifact");
    }

    @Override
    public File getLocalRepositoryDir() {
        return new File(processProperties.getOrDefault("maven.local.repo", LOCAL_REPO_DIR));
    }

    @Override
    public String getOutFileName() {
        return processProperties.get("maven.out.file");
    }

    @Override
    public List<NexusRepository> getNexusRepositories() {
        String prefix = "nexus.repository.";
        List<NexusRepository> repositories = processProperties.findByPrefix(prefix)
                .map(e -> {
                    String key = e.getKey();
                    String name = key.substring(prefix.length());
                    RepositoryType type = RepositoryType.typeOf(key);
                    return new NexusRepository(name, e.getValue(), type);
                })
                .collect(toList());

        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("Can't find nexus repositories. Property prefix: " + prefix);
        }

        return repositories;
    }

    @Override
    public ClasspathStrategyType getClasspathResolveStrategy() {
        String strategy = processProperties.get("mgmt.classpath.resolve.strategy");
        try {
            return strategy == null ? NEXUS : ClasspathStrategyType.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Incorrect classpath strategy: " + strategy.toUpperCase()
                    + ". Available options: " + Arrays.toString(ClasspathStrategyType.values()));
        }
    }

    @Override
    public void changeArtifactVersion(String version) {
        String lastVersion = getArtifact().getVersion();
        Map<String, String> newVersions = processProperties.asMap().entrySet().stream()
                .filter(e -> e.getValue().contains(lastVersion))
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().replace(lastVersion, version)));

        processProperties.update(newVersions);
    }
}
