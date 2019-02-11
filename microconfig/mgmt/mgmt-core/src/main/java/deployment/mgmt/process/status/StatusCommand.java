package deployment.mgmt.process.status;

public interface StatusCommand {
    ServiceStatus getStatus(String service);

    void printStatus(String... services);
}
