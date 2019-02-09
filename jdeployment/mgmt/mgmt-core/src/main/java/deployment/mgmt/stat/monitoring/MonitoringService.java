package deployment.mgmt.stat.monitoring;

public interface MonitoringService {
    MemoryUsage getGroupMemoryUsage();

    MemoryUsage getEnvMemoryUsage();
}
