package deployment.mgmt.configs.filestructure;

import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.util.FileUtils.createDir;
import static deployment.util.FileUtils.createFile;

@RequiredArgsConstructor
public class ServiceLogDirsImpl implements ServiceLogDirs {
    private final ServiceDirs serviceDirs;

    @Override
    public File getLogDir(String service) {
        return createDir(new File(serviceDirs.getServiceDir(service), "logs"));
    }

    @Override
    public File getLogFile(String service, String file) {
        return new File(getLogDir(service), file);
    }

    @Override
    public File getMavenLogFile(String service) {
        return createFile(getLogFile(service, "maven.log"));
    }
}