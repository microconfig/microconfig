package deployment.mgmt.configs.filestructure;

import java.io.File;

public interface ServiceDirs {
    File getComponentsDir();

    File getServiceListFile();

    File getServiceDir(String service);

    File getDiffFile(String service);

    File getServicePropertiesFile(String service);

    File getServiceFile(String service, String file);

    File getPidFile(String service);
}