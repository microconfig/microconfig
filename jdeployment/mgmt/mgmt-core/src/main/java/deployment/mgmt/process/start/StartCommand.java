package deployment.mgmt.process.start;

public interface StartCommand {
    default void start(String... services) {
        startWithArgs(services);
    }

    void startWithArgs(String[] services, String... args);
}
