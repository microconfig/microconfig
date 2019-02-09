package deployment.mgmt.configs.filestructure;

import java.io.File;

public interface ServiceLogDirs {
    File getLogDir(String service);

    File getMavenLogFile(String service);

    File getLogFile(String service, String file);
}
