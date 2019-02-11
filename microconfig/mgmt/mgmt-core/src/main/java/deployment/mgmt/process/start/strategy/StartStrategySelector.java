package deployment.mgmt.process.start.strategy;

import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.StartHandle;
import deployment.mgmt.process.start.StartStrategy;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class StartStrategySelector implements StartStrategy {
    private final List<StartStrategy> startStrategies;

    @Override
    public StartHandle createHandle(String service, String[] args, ProcessProperties processProperties, Map<String, String> envVariables) {
        return startStrategies.stream()
                .filter(s -> s.support(processProperties))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Can't find strategy to start " + service))
                .createHandle(service, args, processProperties, envVariables);
    }

    @Override
    public boolean support(ProcessProperties processProperties) {
        return true;
    }
}