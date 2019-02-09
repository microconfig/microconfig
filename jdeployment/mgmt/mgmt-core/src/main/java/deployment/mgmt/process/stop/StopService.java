package deployment.mgmt.process.stop;

public interface StopService {
    void stopAll();

    void stop(String... services);

    void killAllJava();
}
