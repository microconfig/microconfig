package deployment.mgmt.configs.filestructure;

import java.io.File;

public interface ProcessDirs {
    File getProcessDir(String service);

    File getProcessPropertiesFile(String service);

    File getEnvVariablesFile(String service);

    File getProcessFile(String service, String file);

    File getClasspathFile(String service);

    File getClasspathPrependFile(String service);

    File getClasspathVersionFile(String service);

    File getClasspathDiffFile(String service);

    File createProcessFile(String service, String file);

    File getLastCmdLineFile(String service);

    void removeProcessFile(String service, String file);
}
