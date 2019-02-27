package deployment.mgmt.configs.filestructure;

import java.io.File;

public interface ConfigDirs {
    File getConfigsRootDir();

    File getConfigSourcesRootDir();

    File getConfigVersionFile();

    File getProjectVersionFile(String env);

    File getMgmtArtifactFile(String env);

    File getScriptsDir();

    File getMgmtScriptsDir();

    File getEnvCfgFile();
}
