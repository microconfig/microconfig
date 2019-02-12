package deployment.mgmt.process.stop;

import deployment.mgmt.configs.service.metadata.MetadataProvider;
import deployment.mgmt.configs.service.properties.PropertyService;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static deployment.mgmt.process.stop.StopHandle.create;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class StopCommandImpl implements StopCommand {
    private static final int DEFAULT_STOP_ORDER = MAX_VALUE;

    private final PropertyService propertyService;
    private final MetadataProvider metadataProvider;

    @Override
    public void stop(String... services) {
        long t = currentTimeMillis();

        doStop(services);

        if (services.length > 1) {
            announce("Stopped " + services.length + " services in " + secAfter(t));
        }
    }

    private void doStop(String... services) {
        SortedMap<Integer, List<String>> stopPriorityMap = Stream.of(services)
                .collect(groupingBy(this::getStopOrder, TreeMap::new, toList()));

        List<String> parallelStop = stopPriorityMap.remove(DEFAULT_STOP_ORDER);
        List<String> ordered = stopPriorityMap.values().stream().flatMap(Collection::stream).collect(toList());

        doStop(ordered, false);
        doStop(parallelStop, true);
    }

    private int getStopOrder(String service) {
        try {
            return ofNullable(propertyService.getProcessProperties(service).getStopOrder())
                    .orElse(DEFAULT_STOP_ORDER);
        } catch (RuntimeException e) {
            return DEFAULT_STOP_ORDER;
        }
    }

    private void doStop(List<String> services, boolean parallel) {
        if (services == null || services.isEmpty()) return;

        announce("Stopping" + (parallel ? " in parallel " : " sequentially ") + services);

        Stream<String> stream = parallel ? services.parallelStream() : services.stream();
        stream.forEach(s -> create(s, metadataProvider).ifPresent(StopHandle::stop));
    }
}