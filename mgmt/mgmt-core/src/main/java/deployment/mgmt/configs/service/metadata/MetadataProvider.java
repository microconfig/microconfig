package deployment.mgmt.configs.service.metadata;

import java.util.Optional;

public interface MetadataProvider {
    Optional<Long> lastPid(String service);

    void storePid(String service, long pid);

    void deletePid(String service);

    boolean isLastRunSucceed(String service);

    void updateLastRunStatus(String service, boolean status);

    void onRunFailed(String service, Exception e);
}
