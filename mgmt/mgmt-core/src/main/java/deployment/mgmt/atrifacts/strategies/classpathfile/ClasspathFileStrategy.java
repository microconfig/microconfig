package deployment.mgmt.atrifacts.strategies.classpathfile;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.ArtifactType;
import deployment.mgmt.atrifacts.ClasspathStrategy;
import deployment.mgmt.atrifacts.nexusclient.ArtifactRequestListener;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.service.properties.ClasspathStrategyType;
import deployment.mgmt.configs.service.properties.NexusRepository;
import io.microconfig.utils.Logger;
import lombok.RequiredArgsConstructor;
import mgmt.utils.FileLogger;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

import static deployment.mgmt.configs.service.properties.ClasspathStrategyType.CLASSPATH_FILE;
import static io.microconfig.utils.TimeUtils.percentProgress;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static mgmt.utils.LoggerUtils.oneLineInfo;

@RequiredArgsConstructor
public class ClasspathFileStrategy implements ClasspathStrategy {
    private final JarClasspathReader jarClasspathReader;
    private final UnknownGroupResolver unknownGroupResolver;
    private final NexusClient nexus;

    @Override
    public List<File> downloadDependencies(Artifact artifact, boolean resolveSingleArtifact,
                                           List<NexusRepository> nexusRepositories, File localRepositoryDir,
                                           File logTo) {
        try (FileLogger logger = new FileLogger(logTo)) {
            File serviceJar = downloadIfMissing(artifact, localRepositoryDir, nexusRepositories, logger, () -> 100);
            if (serviceJar == null) {
                throw new IllegalStateException("Can't download main artifact " + artifact);
            }

            if (resolveSingleArtifact) return singletonList(serviceJar);

            List<Artifact> classpath = jarClasspathReader.extractClasspath(serviceJar, artifact);
            List<Artifact> resolved = unknownGroupResolver.resolve(classpath, artifact, nexusRepositories, localRepositoryDir);
            //todo2 print warn if version conflict detected
            return download(resolved, serviceJar, nexusRepositories, localRepositoryDir, logger);
        }
    }

    private List<File> download(List<Artifact> artifacts, File parentJar,
                                List<NexusRepository> nexusRepositories, File localRepositoryDir,
                                FileLogger logger) {
        Supplier<Integer> percentProgress = percentProgress(artifacts.size());
        List<File> dependencies = artifacts.parallelStream()
                .map(jarPath -> downloadIfMissing(jarPath, localRepositoryDir, nexusRepositories, logger, percentProgress))
                .collect(toList());
        dependencies.add(0, parentJar);
        return dependencies;
    }

    private File downloadIfMissing(Artifact artifact, File localRepo,
                                   List<NexusRepository> nexusRepositories,
                                   FileLogger mavenLogger, Supplier<Integer> percentProgress) {
        try {
            return nexus.download(artifact)
                    .from(nexusRepositories)
                    .tryFindIn(localRepo)
                    .withEventListener(logTo(mavenLogger, percentProgress.get()))
                    .asFile();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ArtifactRequestListener logTo(FileLogger mavenLogger, int percent) {
        return new ArtifactRequestListener() {
            @Override
            public void alreadyExists(Artifact artifact, ArtifactType artifactType, File file) {
                mavenLogger.info("Already exists: " + artifact + " " + artifactType);
            }

            @Override
            public void downloaded(Artifact artifact, ArtifactType artifactType, File file) {
                oneLineInfo("Downloaded " + percent + "% " + artifact + " " + artifactType);
                mavenLogger.info("Downloaded " + artifact + " " + artifactType);
            }

            @Override
            public void error(Artifact artifact, ArtifactType artifactType, Exception e) {
                Logger.error("\nArtifact error:" + artifact + " " + artifactType + ", " + e.getMessage());
                mavenLogger.error(e);
            }
        };
    }

    @Override
    public ClasspathStrategyType getType() {
        return CLASSPATH_FILE;
    }
}