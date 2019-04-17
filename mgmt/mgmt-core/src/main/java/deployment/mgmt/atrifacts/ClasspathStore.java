package deployment.mgmt.atrifacts;

import deployment.mgmt.atrifacts.changes.ClasspathDiff;

import java.io.File;
import java.util.List;
import java.util.Optional;

interface ClasspathStore {
    String getClasspath(String service, boolean onlyServiceArtifacts);

    List<File> getClasspathAsFiles(String service, boolean onlyServiceArtifacts);

    void clearClasspath(String service);

    void storeClasspath(String service, List<File> artifacts);

    Optional<String> getClasspathVersion(String service);

    void storeDiff(String service, ClasspathDiff classpathDiff);
}