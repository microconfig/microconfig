package deployment.mgmt.process.start;

import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static deployment.mgmt.process.start.StartHandleImpl.errorResult;
import static deployment.util.ConsoleColor.green;
import static deployment.util.Logger.*;
import static deployment.util.PropertiesUtils.hasSystemFlag;
import static deployment.util.StreamUtils.map;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class StartCommandImpl implements StartCommand {
    private final PropertyService propertyService;
    private final PreStartStep preStartStep;
    private final StartStrategy startStrategy;

    @Override
    public void startWithArgs(String[] services, String... args) {
        announce("Starting " + Arrays.toString(services));

        List<StartHandle> handlers = prepareStart(services, args);
        if (hasSystemFlag("fakeStart")) return;

        Map<Boolean, List<StartHandle>> handlesByParallelStatus = handlers.stream()
                .collect(groupingBy(s -> s.getProcessProperties().allowParallelStart()));

        sequentialStart(handlesByParallelStatus.getOrDefault(false, emptyList()));
        parallelStart(handlesByParallelStatus.getOrDefault(true, emptyList()));
    }

    private void sequentialStart(List<StartHandle> handlers) {
        logStartStatus("Executing cmd line sequentially", handlers);

        handlers.stream()
                .peek(this::executedCmdLine)
                .forEach(this::await);
    }

    //todo start in time no more than cpu count
    private void parallelStart(List<StartHandle> all) {
        Map<Integer, List<StartHandle>> handleGroups = all.stream()
                .collect(groupingBy(s -> s.getProcessProperties().getStartGroup(), TreeMap::new, toList()));

        handleGroups.forEach((group, handles) -> {
            logStartStatus("Executing cmd line in parallel, group[" + group + "]", handles);
            handles.forEach(this::executedCmdLine);
            handles.forEach(this::await);
        });
    }

    private List<StartHandle> prepareStart(String[] services, String... args) {
        List<StartHandle> handlers = stream(services).map(s -> prepareStart(s, args)).collect(toList());
        logLineBreak();
        return handlers;
    }

    private StartHandle prepareStart(String service, String... args) {
        try {
            announce("\nPreparing to start " + service);
            ProcessProperties processProperties = propertyService.getProcessProperties(service);
            preStartStep.beforeStart(service, processProperties);

            return startStrategy.createHandle(service, args, processProperties, propertyService.getEnvVariables(service));
        } catch (RuntimeException e) {
            return errorResult(service, e); //todo2 store metadata error
        }
    }

    private void executedCmdLine(StartHandle startHandle) {
        startHandle.executedCmdLine();
    }

    private void await(StartHandle startHandle) {
        if (startHandle.awaitStartAndGetStatus()) {
            announce("STARTED " + startHandle.getServiceName() + "\n");
        } else {
            error("FAILED to start " + startHandle.getServiceName(), startHandle.getException());
        }
    }

    private void logStartStatus(String message, List<StartHandle> handlers) {
        if (handlers.isEmpty()) return;
        info(green(message + ": ") + map(handlers, StartHandle::getServiceName) + "\n");
    }
}