package deployment.mgmt.atrifacts;

import deployment.mgmt.atrifacts.changes.ClasspathDiff;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.MavenSettings;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.microconfig.utils.FileUtils.*;
import static io.microconfig.utils.IoUtils.firstLineOrEmpty;
import static io.microconfig.utils.IoUtils.readFullyOrEmpty;
import static io.microconfig.utils.Logger.warn;
import static io.microconfig.utils.StringUtils.isEmpty;
import static java.io.File.pathSeparator;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ClasspathStoreImpl implements ClasspathStore {
    private static final String LINE_SEPARATOR = "\n";

    private final DeployFileStructure deployFileStructure;
    private final PropertyService propertyService;

    @Override
    public String getClasspath(String service, boolean onlyServiceArtifacts) {
        String classpath = (onlyServiceArtifacts ? "" : readFullyOrEmpty(classpathPrependFile(service)))
                + readFullyOrEmpty(classpathFile(service));
        return classpath.replace(LINE_SEPARATOR, "");
    }

    @Override
    public List<File> getClasspathAsFiles(String service, boolean onlyServiceArtifacts) {
        String classpath = getClasspath(service, onlyServiceArtifacts);
        if (isEmpty(classpath)) return emptyList();

        File serviceDir = deployFileStructure.service().getServiceDir(service);
        return stream(classpath.split(pathSeparator))
                .map(path -> new File(serviceDir, path))
                .collect(toList());
    }

    @Override
    public void clearClasspath(String service) {
        delete(classpathFile(service));
        delete(classpathVersionFile(service));
        delete(classpathDiffFile(service));
    }

    @Override
    public Optional<String> getClasspathVersion(String service) {
        return ofNullable(firstLineOrEmpty(classpathVersionFile(service)));
    }

    @Override
    public void storeClasspath(String service, List<File> artifacts) {
        ProcessProperties processProperties = propertyService.getProcessProperties(service);

        boolean partialResolved = artifacts.contains(null);
        storeClasspath(service, artifacts, processProperties);
        storeClasspathVersion(service, processProperties.getMavenSettings(), partialResolved);
    }

    @Override
    public void storeDiff(String service, ClasspathDiff classpathDiff) {
        File diffFile = classpathDiffFile(service);

        if (classpathDiff.isEmpty()) {
            delete(diffFile);
        } else {
            warn("Found " + classpathDiff.size() + " classpath changes for " + service);
            write(diffFile, classpathDiff.toString());
        }
    }

    private void storeClasspath(String service, List<File> artifacts, ProcessProperties processProperties) {
        write(classpathPrependFile(service), processProperties.getJavaAppSettings().getClasspathPrepend());
        write(classpathFile(service), join(artifacts, service));

        String outFileName = processProperties.getMavenSettings().getOutFileName();
        if (outFileName != null) {
            copy(classpathFile(service), deployFileStructure.process().getProcessFile(service, outFileName));
        }
    }

    private void storeClasspathVersion(String service, MavenSettings mavenSettings, boolean partialResolved) {
        File classpathVersionFile = classpathVersionFile(service);
        if (partialResolved) {
            warn("Some artifacts haven't been resolved. " + ClassNotFoundException.class.getSimpleName() + " is possible in runtime");
            delete(classpathVersionFile);
        } else {
            write(classpathVersionFile, mavenSettings.getArtifact().getVersion());
        }
    }

    private String join(List<File> artifacts, String service) {
        Path serviceDir = deployFileStructure.service().getServiceDir(service).toPath();
        return artifacts.stream()
                .filter(Objects::nonNull)
                .map(File::toPath)
                .map(serviceDir::relativize)
                .map(Path::toString)
                .collect(joining(pathSeparator + LINE_SEPARATOR));
    }

    private File classpathFile(String service) {
        return deployFileStructure.process().getClasspathFile(service);
    }

    private File classpathPrependFile(String service) {
        return deployFileStructure.process().getClasspathPrependFile(service);
    }

    private File classpathVersionFile(String service) {
        return deployFileStructure.process().getClasspathVersionFile(service);
    }

    private File classpathDiffFile(String service) {
        return deployFileStructure.process().getClasspathDiffFile(service);
    }
}