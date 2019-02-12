package deployment.mgmt.configs.updateconfigs;

import java.util.List;

public interface NewServicePreparer {
    void prepare(List<String> services, boolean skipClasspathBuildForSnapshot);
}
