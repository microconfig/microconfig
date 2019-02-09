package deployment.mgmt.atrifacts.strategies.classpathfile;

import deployment.mgmt.atrifacts.Artifact;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static deployment.mgmt.atrifacts.Artifact.UNKNOWN_GROUP_ID;
import static deployment.util.ZipUtils.readInnerFile;
import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class JarClasspathFileReaderImpl implements JarClasspathReader {
    private static final String GRADLE_DIR = ".gradle/caches/modules-2/";//files-2.1/

    @Override //todo2 return list of missing artifacts
    public List<Artifact> extractClasspath(File artifactFile, Artifact artifact) {
        String classpath = new String(readInnerFile(artifactFile, CLASSPATH_FILE));

        return stream(classpath.split(", "))
//                .peek(Logger::info)
                .map(path -> toArtifact(path, artifact.getVersion()))
                .filter(Objects::nonNull)
//                .peek(a -> Logger.info(a.getMavenFormatString()))
                .collect(toList());
    }

    private Artifact toArtifact(String path, String parentVersion) {
        if (!path.endsWith(".jar")) return null;

        return of(path)
                .filter(this::containsGradlePath)
                .map(this::removeGradleDir)
                .map(this::toArtifactFromPath)
                .orElseGet(() -> {
                    String cleanPath = path.replace("/build/libs/", "/");
                    int versionIndex = cleanPath.indexOf(parentVersion);
                    if (versionIndex < 0) {
                        throw new IllegalArgumentException("Cant' convert path to dependency " + path);
                    }

                    File filePath = new File(cleanPath.substring(0, versionIndex - 1));
                    String artifactId = filePath.getName();
                    String groupIdPart = filePath.getParentFile().getParentFile().getName();
                    return Artifact.fromMavenString(UNKNOWN_GROUP_ID + groupIdPart + ":" + artifactId + ":" + parentVersion);
                });
    }

    private boolean containsGradlePath(String path) {
        return path.contains(GRADLE_DIR);
    }

    private String removeGradleDir(String artifact) {
        int gradleDirIndex = artifact.indexOf(GRADLE_DIR);
        return artifact.substring(artifact.indexOf("/", gradleDirIndex + GRADLE_DIR.length()) + 1);
    }

    private Artifact toArtifactFromPath(String path) {
        String line = removeHashDirAndFileName(path).replace("/", ":");
        return Artifact.fromMavenString(line);
    }

    private String removeHashDirAndFileName(String artifact) {
        int beforeLast = artifact.lastIndexOf('/', artifact.lastIndexOf('/') - 1);
        return artifact.substring(0, beforeLast);
    }
}