package deployment.mgmt.process.start.prestart;

import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.PreStartStep;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CompositePreStar implements PreStartStep {
    private final List<PreStartStep> steps;

    @Override
    public void beforeStart(String service, ProcessProperties processProperties) {
        steps.forEach(l -> l.beforeStart(service, processProperties));
    }
}
