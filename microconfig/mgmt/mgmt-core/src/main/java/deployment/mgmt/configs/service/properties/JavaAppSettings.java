package deployment.mgmt.configs.service.properties;

import java.util.List;

public interface JavaAppSettings {
    String getJavaPath();

    String getJavaOpts();

    List<String> getJavaOptsAsList();

    String getClasspathPrepend();

    String getProcessArgs();

    List<String> getProcessArgsAsList();

    String getMainClass();
}
