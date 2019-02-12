package deployment.mgmt.configs.componentgroup;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.PropertyService;
import deployment.mgmt.process.status.ExecutionStatus;
import deployment.mgmt.process.status.StatusCommand;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static deployment.mgmt.configs.service.properties.impl.StandardServiceGroup.*;
import static io.microconfig.utils.ConsoleColor.green;
import static io.microconfig.utils.Logger.info;
import static java.util.Map.entry;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ServiceGroupManagerImpl implements ServiceGroupManager {
    private final ComponentGroupService componentGroupService;
    private final StatusCommand statusCommand;
    private final DeployFileStructure deployFileStructure;
    private final PropertyService propertyService;

    @Override
    public List<String> findServicesByGroup(String... group) {
        return componentGroupService.getServices().stream()
                .filter(containsAny(group))
                .collect(toList());
    }

    private Predicate<String> containsAny(String... groups) {
        return service -> of(groups).anyMatch(assignedTo(service));
    }

    private Predicate<String> assignedTo(String service) {
        return group -> {
            if (FAILED.nameEquals(group)) {
                return statusCommand.getStatus(service).getValue() == ExecutionStatus.FAILED;
            }

            if (STOPPED.nameEquals(group)) {
                ExecutionStatus status = statusCommand.getStatus(service).getValue();
                return (status == ExecutionStatus.STOPPED || status == ExecutionStatus.NOT_EXECUTED) && !propertyService.getProcessProperties(service).isTask();
            }

            if (CHANGED.nameEquals(group)) {
                return deployFileStructure.service().getDiffFile(service).exists()
                        || deployFileStructure.process().getClasspathDiffFile(service).exists();
            }

            return propertyService.getProcessProperties(service).getGroups().contains(group);
        };
    }

    @Override
    public void printServiceGroups() {
        Stream<Entry<String, String>> serviceGroups = componentGroupService.getServices().stream()
                .flatMap(s -> propertyService.getProcessProperties(s).getGroups().stream().map(g -> entry(g, s)));
        Stream<Entry<String, String>> runtimeGroups = runtimeGroups().stream()
                .flatMap(g -> findServicesByGroup(g.groupName()).stream().map(s -> entry(g.groupName(), s)));

        Map<String, List<String>> groupToServices = concat(serviceGroups, runtimeGroups)
                .collect(groupingBy(Entry::getKey, TreeMap::new, mapping(Entry::getValue, toList())));

        groupToServices.forEach((group, services) -> info(green(group) + " -> " + services + "\n"));
    }
}