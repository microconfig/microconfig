package deployment.mgmt.atrifacts;

import deployment.mgmt.atrifacts.ClasspathRequest.Classpath;
import deployment.mgmt.atrifacts.changes.ClasspathDiff;
import deployment.mgmt.atrifacts.changes.ClasspathDiffBuilder;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.MavenSettings;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static deployment.util.Logger.error;
import static deployment.util.Logger.logLineBreak;
import static deployment.util.LoggerUtils.oneLineInfo;
import static deployment.util.TimeUtils.msAfter;
import static java.lang.System.currentTimeMillis;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
public class ClasspathServiceImpl implements ClasspathService {
    private final DeployFileStructure deployFileStructure;
    private final ClasspathStore classpathStore;
    private final ClasspathStrategySelector classpathStrategySelector;

    @Override
    public ClasspathRequest classpathFor(String service) {
        return new ClasspathRequestImpl(service, false);
    }

    @Wither(PRIVATE)
    @RequiredArgsConstructor
    private class ClasspathRequestImpl implements ClasspathRequest {
        private final String service;
        private final boolean skipIfSnapshot;

        @Override
        public ClasspathRequest skipIfSnapshot(boolean flag) {
            return withSkipIfSnapshot(flag);
        }

        @Override
        public Classpath current() {
            return new ClasspathImpl(service);
        }

        @Override
        public Classpath buildUsing(ProcessProperties processProperties) {
            if (updateNeeded(processProperties)) {
                buildClasspath(processProperties);
            }

            return current();
        }

        private boolean updateNeeded(ProcessProperties processProperties) {
            Artifact artifact = processProperties.getMavenSettings().getArtifact();
            if (artifact.isSnapshot()) {
                return !skipIfSnapshot || processProperties.isTask();
            }

            Predicate<String> classpathMissing = version -> {
                Optional<String> currentVersion = classpathStore.getClasspathVersion(service);
                return !currentVersion.isPresent() || !currentVersion.get().equals(version);
            };

            return classpathMissing.test(artifact.getVersion());
        }

        private void buildClasspath(ProcessProperties processProperties) {
            ClasspathDiffBuilder diffBuilder = ClasspathDiff.builder(service).indexPreviousClasspath(current().asFiles());
            doBuildClasspath(processProperties.getMavenSettings());
            diffBuilder.indexCurrentClasspath(current().asFiles());

            classpathStore.storeDiff(service, diffBuilder.compare());
        }

        private void doBuildClasspath(MavenSettings mavenSettings) {
            try {
                long t = currentTimeMillis();
                classpathStore.clearClasspath(service);

                List<File> artifacts = downloadArtifacts(mavenSettings);
                classpathStore.storeClasspath(service, artifacts);

                oneLineInfo("Resolved " + artifacts.size() + " dependencies for " + service + " in " + msAfter(t));
                logLineBreak();
            } catch (RuntimeException e) {
                error("Failed to update classpath for " + service + ", " + e.getMessage());
            }
        }

        private List<File> downloadArtifacts(MavenSettings mavenSettings) {
            ClasspathStrategy strategy = classpathStrategySelector.selectStrategy(service, mavenSettings);
            return strategy.downloadDependencies(mavenSettings, deployFileStructure.logs().getMavenLogFile(service));
        }
    }

    @RequiredArgsConstructor
    private class ClasspathImpl implements Classpath {
        private final String service;

        @Override
        public String asString() {
            return classpathStore.getClasspath(service);
        }

        @Override
        public List<File> asFiles() {
            return classpathStore.getClasspathAsFiles(service);
        }
    }
}