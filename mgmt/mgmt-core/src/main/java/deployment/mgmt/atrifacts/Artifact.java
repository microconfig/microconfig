package deployment.mgmt.atrifacts;

import deployment.mgmt.version.Version;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static deployment.mgmt.atrifacts.ArtifactType.JAR;
import static deployment.mgmt.atrifacts.ArtifactType.POM;
import static io.microconfig.utils.FileUtils.userHome;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.of;

@Getter
@RequiredArgsConstructor
public class Artifact {
    private static final String LOCAL_REPO_NAME = "maven-repo";

    public static final String LOCAL_REPO_DIR = userHome() + "/" + LOCAL_REPO_NAME;
    public static final String UNKNOWN_GROUP_ID = "?";
    public static final String SNAPSHOT = "-SNAPSHOT";

    private final String mavenFormatString;

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String version;

    public Artifact(String groupId, String artifactId, String classifier, String version) {
        this(toMavenString(groupId, artifactId, classifier, version), groupId, artifactId, classifier, version);
    }

    public static Artifact fromMavenString(String original) {
        if (original == null) return null;

        String[] parts = original.split(":");
        String classifierAndType = parts.length == 3 ? null : join(":", asList(parts).subList(2, parts.length - 1));
        return new Artifact(original, parts[0], parts[1], classifierAndType, parts[parts.length - 1]);
    }

    public static Artifact fromFile(File file) {
        String path = file.getPath().replace("\\", "/");

        try {
            int mavenRepoIndex = path.indexOf(LOCAL_REPO_NAME);
            mavenRepoIndex = mavenRepoIndex < 0 ? path.startsWith("/") ? 1 : 0 : mavenRepoIndex + LOCAL_REPO_NAME.length() + 1;

            int nameIndex = path.lastIndexOf('/');
            int versionIndex = path.lastIndexOf('/', nameIndex - 1);
            String version = path.substring(versionIndex + 1, nameIndex);
            String name = path.substring(nameIndex + 1);
            int nameVersionIndex = name.indexOf(version);
            String artifactId = name.substring(0, nameVersionIndex - 1);
            String classifier = name.substring(nameVersionIndex + version.length() + 1);
            if (classifier.equals(JAR.name().toLowerCase()) || classifier.equals(POM.name().toLowerCase())) {
                classifier = null;
            }
            String groupId = path.substring(mavenRepoIndex, versionIndex - artifactId.length() - 1).replace('/', '.');

            return new Artifact(groupId, artifactId, classifier, version);
        } catch (RuntimeException e) {
            return new Artifact(path, UNKNOWN_GROUP_ID, UNKNOWN_GROUP_ID, UNKNOWN_GROUP_ID);
        }
    }

    public boolean isSnapshot() {
        return version.contains(SNAPSHOT);
    }

    public Artifact withMaxVersion(String version) {
        if (version == null) return this;

        return olderThan(version) ? new Artifact(toMavenString(groupId, artifactId, classifier, version), groupId, artifactId, classifier, version) : this;
    }

    public Artifact withNewVersion(String version) {
        return new Artifact(
                toMavenString(groupId, artifactId, classifier, version),
                groupId, artifactId, classifier, version
        );
    }

    public Artifact withNewGroupId(String groupId) {
        return new Artifact(
                toMavenString(groupId, artifactId, classifier, version),
                groupId, artifactId, classifier, version
        );
    }

    public Artifact withoutClassifier() {
        return new Artifact(
                toMavenString(groupId, artifactId, null, version),
                groupId, artifactId, null, version
        );
    }

    private static String toMavenString(String groupId, String artifactId, String classifier, String version) {
        return of(groupId, artifactId, classifier, version)
                .filter(Objects::nonNull)
                .collect(joining(":"));
    }

    public boolean olderThan(String version) {
        return new Version(this.version).olderThan(version);
    }

    public boolean olderThan(String version, boolean trueIfEquals) {
        return new Version(this.version).olderThan(version, trueIfEquals);
    }

    public boolean hasUnknownGroupId() {
        return groupId.startsWith(UNKNOWN_GROUP_ID);
    }

    public String toUrlPath(ArtifactType artifactType) {
        String type = detectTypeOrElse(artifactType);
        return baseUrl() + version + "/" + artifactId + "-" + version + type;
    }

    public String baseUrl() {
        return groupId.replace(".", "/") + "/" + artifactId + "/";
    }

    private String detectTypeOrElse(ArtifactType artifactType) {
        if (classifier == null) return artifactType.extension();
        return (classifier.contains(":") ? "-" : ".") + classifierUrl();
    }

    private String classifierUrl() {
        List<String> list = asList(classifier.split(":"));
        reverse(list);
        return join(".", list);
    }

    public boolean isArchive() {
        return classifier != null && (classifier.contains("gz") || classifier.contains("zip"));
    }

    public String simpleFileName() {
        return artifactId + "-" + version + detectTypeOrElse(JAR);
    }

    @Override
    public String toString() {
        return mavenFormatString;
    }

    public List<String> filterNewReleases(Stream<String> allVersions, boolean includeCurrentVersion) {
        return new Version(version).filterNewReleases(allVersions, includeCurrentVersion, 5);
    }
}