package deployment.mgmt.atrifacts.nexusclient;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.ArtifactType;
import deployment.mgmt.configs.deploysettings.Credentials;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.service.properties.NexusRepository;
import io.microconfig.utils.IoUtils;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static deployment.mgmt.atrifacts.Artifact.LOCAL_REPO_DIR;
import static deployment.mgmt.atrifacts.Artifact.SNAPSHOT;
import static deployment.mgmt.atrifacts.ArtifactType.JAR;
import static io.microconfig.utils.FileUtils.createDir;
import static io.microconfig.utils.FileUtils.delete;
import static mgmt.utils.ByteReaderUtils.readAllBytes;
import static io.microconfig.utils.Logger.warn;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
public class NexusClientImpl implements NexusClient {
    private static final Pattern versionPattern = Pattern.compile("<version>(.+)</version>");
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final RepositoryPriorityService repositoryPriorityService;
    private final DeploySettings deploySettings;

    private final Set<File> downloadedSnapshots = new ConcurrentSkipListSet<>();

    @Override
    public ArtifactRequest download(Artifact artifact) {
        return new ArtifactRequestImpl(artifact, JAR, emptyList(), new File(LOCAL_REPO_DIR), ArtifactRequestListener.empty());
    }

    @Override
    public List<String> newVersionsFor(Artifact artifact, NexusRepository repository,
                                       boolean includeCurrentVersion) {

        Supplier<String> requestMetadata = () -> {
            String metadataUrl = repository.getUrl() + "/" + artifact.baseUrl() + "/maven-metadata.xml";
            try {
                return IoUtils.readFully(getInputStream(metadataUrl));
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't fetch versions for " + artifact + " from " + metadataUrl);
            }
        };

        Stream<String> allVersions = versionPattern.matcher(requestMetadata.get())
                .results()
                .map(m -> m.group(1));

        return artifact.filterNewReleases(allVersions, includeCurrentVersion);
    }

    @Wither(PRIVATE)
    @RequiredArgsConstructor
    private class ArtifactRequestImpl implements ArtifactRequest {
        private final Artifact artifact;
        private final ArtifactType artifactType;
        private final List<NexusRepository> repositories;
        private final File localRepoDir;
        private final ArtifactRequestListener listener;

        @Override
        public ArtifactRequest withType(ArtifactType artifactType) {
            return withArtifactType(artifactType);
        }

        @Override
        public ArtifactRequest from(List<NexusRepository> repositories) {
            return withRepositories(repositories);
        }

        @Override
        public ArtifactRequest tryFindIn(File localRepoDir) {
            return withLocalRepoDir(localRepoDir);
        }

        @Override
        public ArtifactRequest withEventListener(ArtifactRequestListener listener) {
            return withListener(listener);
        }

        @Override
        public File asFile() {
            File saveTo = new File(localRepoDir, artifact.toUrlPath(artifactType));
            if (alreadyExists(saveTo)) {
                listener.alreadyExists(artifact, artifactType, saveTo);
                return saveTo;
            }

            to(saveTo);
            return saveTo;
        }

        private boolean alreadyExists(File saveTo) {
            return saveTo.exists() && (!saveTo.getName().contains(SNAPSHOT) || !downloadedSnapshots.add(saveTo));
        }

        @Override
        public void to(File saveTo) {
            try (InputStream inputStream = urlStream(artifact, artifactType, repositories)) {
                createDir(saveTo.getParentFile());
                copy(inputStream, saveTo.toPath(), REPLACE_EXISTING);
                listener.downloaded(artifact, artifactType, saveTo);
            } catch (AccessDeniedException e) {
                warn("Missing permissions to write to " + saveTo);
                listener.error(artifact, artifactType, e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                listener.error(artifact, artifactType, e);
                delete(saveTo);
                throw new RuntimeException(e);
            }
        }

        @Override
        public byte[] asBytes() {
            return readAllBytes(asFile());
        }

        @Override
        public String asString() {
            return new String(asBytes());
        }

        private InputStream urlStream(Artifact artifact, ArtifactType artifactType, List<NexusRepository> repositories) {
            if (repositories.isEmpty()) {
                throw new IllegalStateException("Please specify nexus repositories");
            }

            List<NexusRepository> prioritized = repositoryPriorityService.withPriority(repositories, artifact);
            for (NexusRepository repository : prioritized) {
                String fullUrl = getFullUrl(repository, artifact, artifactType);

                try {
                    return getInputStream(fullUrl);
                } catch (FileNotFoundException ignore) {
                    warn("\rArtifact " + artifact + " is not found on " + fullUrl + ". Trying next repository...");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            throw new IllegalStateException("Can't download " + artifact + " from " + prioritized);
        }

        private String getFullUrl(NexusRepository nexusRepository, Artifact artifact, ArtifactType artifactType) {
            if (artifact.isSnapshot()) {
                return nexusRepository.getNexusBaseUrl() + "/service/local/artifact/maven/redirect?"
                        + "r=" + nexusRepository.getRepositoryName()
                        + "&g=" + artifact.getGroupId()
                        + "&a=" + artifact.getArtifactId()
                        + "&v=" + artifact.getVersion()
                        + "&e=" + artifactType.name().toLowerCase();
            }

            return nexusRepository.getUrl() + "/" + artifact.toUrlPath(artifactType);
        }
    }

    private InputStream getInputStream(String fullUrl) throws IOException {
        URLConnection urlConnection = new URL(fullUrl).openConnection();
        Credentials credentials = deploySettings.getNexusCredentials();
        if (!credentials.isEmpty()) {
            urlConnection.setRequestProperty(AUTHORIZATION_HEADER, credentials.getBasicAuthorization());
        }
        return urlConnection.getInputStream();
    }
}