package deployment.mgmt.process.stop;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StopServiceImpl implements StopService {
    private final ComponentGroupService componentGroupService;
    private final StopCommand stopCommand;
    private final KillCommand killCommand;

    @Override
    public void stopAll() {
        stop(componentGroupService.getServices().toArray(new String[0]));
    }

    @Override
    public void stop(String... services) {
        stopCommand.stop(services);
    }

    @Override
    public void killAllJava() {
        killCommand.killAllJava();
    }
}
