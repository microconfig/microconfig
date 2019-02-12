package deployment.mgmt.configs.componentgroup;

import java.util.List;
import java.util.Optional;

public interface ComponentGroupService {
    void update(GroupDescription description);

    void updateConfigVersion(String configVersion);

    void updateProjectVersion(String serviceVersion);

    String getProjectVersion();

    GroupDescription getDescription();

    default String getEnv() {
        return getDescription().getEnv();
    }


    List<String> getServices();

    void changeServiceVersion(ServiceDescription newService);


    List<ServiceDescription> getAlteredServices();

    Optional<ServiceDescription> getAlteredVersionService(String service);

    void replaceServiceVersionWithAltered(String... services);

    void cleanAlteredVersions(String... services);
}