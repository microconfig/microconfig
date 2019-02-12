package deployment.mgmt.stat.releases;

import deployment.mgmt.atrifacts.Artifact;
import deployment.mgmt.atrifacts.nexusclient.NexusClient;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.fetch.ConfigFetcher;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static deployment.mgmt.stat.releases.ReadyReleasesImpl.of;
import static deployment.mgmt.stat.releases.ReleaseType.SERVICE;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.List.of;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
public class ReadyReleasesServiceImpl implements ReadyReleasesService {
    private static final String UNKNOWN_VERSION = "?";

    private final PropertyService propertyService;
    private final DeploySettings deploySettings;
    private final NexusClient nexusClient;
    private final ConfigFetcher configFetcher;

    @Override
    public ReadyReleases currentReleaseToServices(List<String> services) {
        UnaryOperator<String> currentVersion = service -> {
            ProcessProperties properties = propertyService.getProcessProperties(service);
            return properties.getConfigVersion() + "," + properties.getVersion();
        };
        return of(services.stream().collect(groupingBy(currentVersion)));
    }

    @Override
    public ReadyReleases serviceToNewReleases(List<String> services) {
        return of(collectServiceToNewReleases(services, SERVICE, true));
    }

    @Override
    public ReadyReleases newReleaseToServices(List<String> services, ReleaseType releaseType) {
        Function<List<String>, String> newVersion = versions -> {
            boolean emptyVersions = versions.isEmpty() || versions.get(0).equals(UNKNOWN_VERSION);
            return emptyVersions ? "" : versions.get(0);
        };

        return of(
                collectServiceToNewReleases(services, releaseType, false)
                        .entrySet()
                        .stream()
                        .collect(groupingBy(
                                e -> newVersion.apply(e.getValue()),
                                mapping(Map.Entry::getKey, toList()))
                        )
        );
    }

    private Map<String, List<String>> collectServiceToNewReleases(List<String> services, ReleaseType releaseType, boolean includeCurrentVersion) {
        Function<String, List<String>> releaseFetcher = releaseType == SERVICE ?
                s -> newServiceReleasesFor(s, includeCurrentVersion)
                : s -> newConfigReleasesFor(s, includeCurrentVersion);

        return services.stream().collect(toLinkedMap(identity(), s -> {
            try {
                return releaseFetcher.apply(s);
            } catch (RuntimeException e) {
                return of(UNKNOWN_VERSION);
            }
        }));
    }

    private List<String> newServiceReleasesFor(String service, boolean includeCurrentVersion) {
        Artifact artifact = propertyService.getProcessProperties(service).getMavenSettings().getArtifact();
        return nexusClient.newVersionsFor(artifact, deploySettings.getNexusReleaseRepository(), includeCurrentVersion);
    }

    private List<String> newConfigReleasesFor(String service, boolean includeCurrentVersion) {
        return configFetcher.newConfigReleases(service, includeCurrentVersion);
    }
}