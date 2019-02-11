package deployment.mgmt.stat.monitoring;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.process.status.ServiceStatus;
import deployment.mgmt.process.status.StatusCommand;
import deployment.mgmt.ssh.SshCommand;
import io.microconfig.environment.ComponentGroup;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static deployment.mgmt.process.status.ExecutionStatus.RUNNING;
import static deployment.mgmt.stat.monitoring.MemoryUsage.statFor;
import static deployment.util.JsonUtil.parse;
import static deployment.util.ProcessUtil.executeAndReadOutput;
import static java.lang.Integer.parseInt;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {
    private final ComponentGroupService componentGroupService;
    private final SshCommand sshCommand;
    private final StatusCommand statusCommand;

    @Override
    public MemoryUsage getEnvMemoryUsage() {
        String env = componentGroupService.getEnv();

        Map<String, ComponentGroup> groupByName = new ConcurrentHashMap<>();
        List<MemoryUsage> groupUsages = sshCommand.executeOnEveryNode(env, "mgmt memory-usage-json", (cg, json) -> {
            groupByName.put(cg.getName(), cg);
            try {
                MemoryUsage memoryUsage = parse(json, MemoryUsage.class);
                requireNonNull(memoryUsage.getName());
                return memoryUsage;
            } catch (RuntimeException e) {
                return statFor(cg.getName(), "group", -1);
            }
        });

        List<MemoryUsage> hostUsages = groupUsages.stream()
                .collect(groupingBy(g -> {
                    ComponentGroup componentGroup = requireNonNull(groupByName.get(g.getName()), () -> "Can't find cg by name " + g.getName());
                    return componentGroup.getIp().orElse("x.x.x.x");
                }))
                .entrySet().stream()
                .map(e -> statFor(e.getKey(), "host", e.getValue().stream().sorted(memoryUsageComparator()).collect(toList())))
                .sorted(memoryUsageComparator())
                .collect(toList());

        return statFor(env, "env", hostUsages);
    }

    @Override
    public MemoryUsage getGroupMemoryUsage() {
        List<MemoryUsage> serviceUsages = componentGroupService.getServices()
                .stream()
                .map(statusCommand::getStatus)
                .map(this::getServiceMemoryUsage)
                .sorted(memoryUsageComparator())
                .collect(toList());

        return statFor(componentGroupService.getDescription().getGroup(), "group", serviceUsages);
    }

    private MemoryUsage getServiceMemoryUsage(ServiceStatus s) {
        return statFor(
                s.getServiceDescription().getName(),
                s.getServiceDescription().getVersion(),
                s.getValue() == RUNNING ? memoryUsageByPid(s.getPid()) : 0
        );
    }

    private int memoryUsageByPid(long parentPid) {
        String psMemoryUsage = executeAndReadOutput("ps", "-p", String.valueOf(parentPid), "-o", "rss").replaceAll("\\D", "");
        int usage = psMemoryUsage.isEmpty() ? 0 : parseInt(psMemoryUsage) / 1024;

        return usage + ProcessHandle.of(parentPid).stream()
                .flatMap(ProcessHandle::children)
                .mapToInt(child -> memoryUsageByPid(child.pid()))
                .sum();
    }

    private Comparator<MemoryUsage> memoryUsageComparator() {
        return comparing(MemoryUsage::getValueInMb).reversed();
    }
}