package deployment.mgmt.configs.filestructure;

import deployment.util.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.util.FileUtils.createFile;

@RequiredArgsConstructor
public class ProcessDirsImpl implements ProcessDirs {
    private final ServiceDirs serviceDirs;

    @Override
    public File getProcessDir(String service) {
        return new File(serviceDirs.getServiceDir(service), ".mgmt");
    }

    @Override
    public File getProcessPropertiesFile(String service) {
        return getProcessFile(service, "process.properties");
    }

    @Override
    public File getEnvVariablesFile(String service) {
        return getProcessFile(service, "env.properties");
    }

    @Override
    public File getClasspathVersionFile(String service) {
        return getProcessFile(service, "classpath_version.mgmt");
    }

    @Override
    public File getClasspathDiffFile(String service) {
        return getProcessFile(service, "classpath_diff.mgmt");
    }

    @Override
    public File getLastCmdLineFile(String service) {
        return serviceDirs.getServiceFile(service, "last_cmd_line.mgmt");
    }

    @Override
    public File getClasspathFile(String service) {
        return getProcessFile(service, "classpath.mgmt");
    }

    @Override
    public File getClasspathPrependFile(String service) {
        return getProcessFile(service, "classpath_prepend.mgmt");
    }

    @Override
    public File getProcessFile(String service, String file) {
        return new File(getProcessDir(service), file);
    }

    @Override
    public void removeProcessFile(String service, String file) {
        FileUtils.delete(getProcessFile(service, file));
    }

    @Override
    public File createProcessFile(String service, String file) {
        return createFile(getProcessFile(service, file));
    }
}