package deployment.mgmt.update.updater;

public interface MgmtAutoUpdater {
    default void updateAndRestart(String... args) {
        updateAndRestart(null, false, args);
    }

    void updateAndRestart(String minVersion, boolean forceUpdateIfSameVersion, String... args);

    void registerRestartCommand(String... args);
}
