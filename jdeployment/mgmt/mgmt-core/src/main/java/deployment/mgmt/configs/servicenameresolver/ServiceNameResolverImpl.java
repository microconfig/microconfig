package deployment.mgmt.configs.servicenameresolver;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

import static deployment.util.Logger.warn;
import static java.lang.System.exit;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ServiceNameResolverImpl implements ServiceNameResolver {
    private final DeploySettings deploySettings;
    private final ComponentGroupService componentGroupService;
    private final PropertyService propertyService;

    @Override
    public String[] resolve(String[] serviceNamePatterns) {
        return deploySettings.strictModeEnabled() ? strictResolve(serviceNamePatterns) : notStrictResolve(serviceNamePatterns);
    }

    @Override
    public String[] resolveWithoutTasks(String[] serviceNamePatterns) {
        String[] names = resolve(serviceNamePatterns);

        return serviceNamePatterns.length == 0 || allArgUsed(serviceNamePatterns) ?
                removeTaskServices(names) : names;
    }

    private String[] removeTaskServices(String[] names) {
        return Stream.of(names)
                .filter(s -> !propertyService.getProcessProperties(s).isTask())
                .toArray(String[]::new);
    }

    @Override
    public String resolveOne(String service) {
        if (componentGroupService.getServices().contains(service)) return service;

        String resolved = notStrictResolve(service)[0];
        requireCorrectName(resolved);
        return resolved;
    }

    @Override
    public String[] notStrictResolve(String... serviceNamePatterns) {
        if (serviceNamePatterns.length == 0 || allArgUsed(serviceNamePatterns)) {
            return componentGroupService.getServices().toArray(new String[0]);
        }

        String[] services = Stream.of(serviceNamePatterns)
                .flatMap(this::findLike)
                .distinct()
                .toArray(String[]::new);

        if (services.length == 0) {
            logBadServiceName(serviceNamePatterns);
        }

        return services;
    }

    @Override
    public void requireCorrectName(String service) {
        if (!componentGroupService.getServices().contains(service)) {
            logBadServiceName(service);
        }
    }

    private String[] strictResolve(String[] serviceNamePatterns) {
        if (serviceNamePatterns.length == 0) {
            logStrictModeWarn(emptyList());
        }

        if (allArgUsed(serviceNamePatterns)) {
            return notStrictResolve(serviceNamePatterns);
        }

        List<String> deployedServiceNames = componentGroupService.getServices();
        List<String> badServiceNames = Stream.of(serviceNamePatterns).filter(s -> !deployedServiceNames.contains(s)).collect(toList());

        if (!badServiceNames.isEmpty()) {
            logStrictModeWarn(badServiceNames);
        }

        return Stream.of(serviceNamePatterns).distinct().toArray(String[]::new);
    }

    private Stream<String> findLike(String pattern) {
        return componentGroupService.getServices()
                .stream()
                .filter(name -> name.toLowerCase().startsWith(pattern.toLowerCase()));
    }

    private boolean allArgUsed(String... serviceNamePattern) {
        return asList(serviceNamePattern).contains(ALL_SERVICE_ALIAS);
    }

    private void logStrictModeWarn(List<String> badServiceNames) {
        warn("Strict mode is enabled");
        if (!badServiceNames.isEmpty()) {
            warn("Can't find services by name " + badServiceNames);
        }
        warn("1) specify exact service names");
        warn("2) or disable strict mode [mgmt strict-mode off]");
        warn("3) or use '" + ALL_SERVICE_ALIAS + "' argument (mgmt restart " + ALL_SERVICE_ALIAS + ")");

        exit(-1);
    }

    private void logBadServiceName(String... serviceNamePatterns) {
        warn("No matching service found: " + String.join(" ", serviceNamePatterns));
        exit(-1);
    }
}