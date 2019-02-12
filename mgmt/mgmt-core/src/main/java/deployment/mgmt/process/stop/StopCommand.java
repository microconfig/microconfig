package deployment.mgmt.process.stop;

public interface StopCommand {
    void stop(String... services);
}
