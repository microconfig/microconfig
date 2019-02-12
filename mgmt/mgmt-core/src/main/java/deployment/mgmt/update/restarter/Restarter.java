package deployment.mgmt.update.restarter;

import java.io.File;

public interface Restarter {
    void restart(File jar, String[] args);

    void registerRestartCommand(String[] args);
}