package deployment.mgmt.configs.componentgroup;

import java.util.List;

public interface ServiceGroupManager {
    List<String> findServicesByGroup(String... groups);

    void printServiceGroups();
}