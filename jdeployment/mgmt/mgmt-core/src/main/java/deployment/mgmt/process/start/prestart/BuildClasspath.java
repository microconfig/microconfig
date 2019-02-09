package deployment.mgmt.process.start.prestart;

import deployment.mgmt.atrifacts.ClasspathService;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.PreStartStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuildClasspath implements PreStartStep {
    private final ClasspathService classpathService;

    @Override
    public void beforeStart(String service, ProcessProperties processProperties) {
        classpathService.classpathFor(service).buildUsing(processProperties);
    }
}