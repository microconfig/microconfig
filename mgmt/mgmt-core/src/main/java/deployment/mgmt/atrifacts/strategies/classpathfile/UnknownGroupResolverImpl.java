package deployment.mgmt.atrifacts.strategies.classpathfile;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.service.properties.NexusRepository;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;

import static deployment.mgmt.atrifacts.Artifact.UNKNOWN_GROUP_ID;
import static deployment.mgmt.atrifacts.ArtifactType.POM;
import static deployment.mgmt.utils.CollectionUtils.findDuplicates;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.map;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

@RequiredArgsConstructor
public class UnknownGroupResolverImpl implements UnknownGroupResolver {
    private final NexusClient nexus;

    @Override
    public List<Artifact> resolve(List<Artifact> artifacts, Artifact parentArtifact, List<NexusRepository> nexusRepositories, File localRepositoryDir) {
        List<Artifact> unknown = filter(artifacts, Artifact::hasUnknownGroupId);
        if (unknown.isEmpty()) return artifacts;

        List<Artifact> resolved = doResolve(unknown, parentArtifact, nexusRepositories, localRepositoryDir);
        return replaceUnknown(artifacts, resolved);
    }

    private List<Artifact> doResolve(List<Artifact> unknown, Artifact parentArtifact,
                                     List<NexusRepository> nexusRepositories, File localRepo) {
        List<Artifact> resolved = new ArrayList<>();
        Queue<Artifact> pomToDownload = new ArrayDeque<>(List.of(parentArtifact));
        Set<String> notUniqueArtifactIds = findDuplicates(map(unknown, Artifact::getArtifactId));

        while (!pomToDownload.isEmpty()) {
            String pom = downloadPom(pomToDownload.remove(), nexusRepositories, localRepo);
            List<Artifact> newResolved = doResolve(unknown, notUniqueArtifactIds, pom);

            resolved.addAll(newResolved);
            pomToDownload.addAll(newResolved);
        }

        if (!unknown.isEmpty()) {
            error("Some artifact haven't been resolved:" + unknown);
        }
        return resolved;
    }

    private String downloadPom(Artifact artifact, List<NexusRepository> nexusRepositories, File localRepo) {
        return nexus.download(artifact)
                .withType(POM)
                .from(nexusRepositories)
                .tryFindIn(localRepo)
                .asString();
    }

    private List<Artifact> doResolve(List<Artifact> unknown, Set<String> notUniqueArtifactIds, String pom) {
        List<Artifact> resolved = new ArrayList<>();

        for (Iterator<Artifact> iterator = unknown.iterator(); iterator.hasNext(); ) {
            Artifact current = iterator.next();

            int artifactIdIndex = pom.indexOf("<artifactId>" + current.getArtifactId() + "</artifactId>");
            if (artifactIdIndex < 0) continue;

            int dependencyIndex = pom.lastIndexOf("<dependency>", artifactIdIndex);
            String groupIdTag = "<groupId>";
            int groupIdIndex = pom.indexOf(groupIdTag, dependencyIndex);
            String groupId = pom.substring(groupIdIndex + groupIdTag.length(), pom.indexOf("</groupId>", groupIdIndex));

            if (notUniqueArtifactIds.contains(current.getArtifactId())
                    && !groupId.endsWith(current.getGroupId().replaceFirst("^\\" + UNKNOWN_GROUP_ID, ""))) {
                continue;
            }

            iterator.remove();
            resolved.add(current.withNewGroupId(groupId));
        }

        return resolved;
    }

    private List<Artifact> replaceUnknown(List<Artifact> artifacts, List<Artifact> resolved) {
        return concat(resolved.stream(), artifacts.stream()) //tood2 it changes order or artifacts in classpath
                .filter(a -> !a.hasUnknownGroupId())
                .collect(toList());
    }
}