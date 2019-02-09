package deployment.mgmt.configs.filestructure;

import java.io.File;

public interface DeployDirs {
    File getDeploySettingsDir();

    File getSecretPropertiesFile();

    File getEncryptionKeyFile();

    File getMgmtScriptFile();

    File getMgmtJarFile();

    File getGroupDescriptionFile();

    File getPostMgmtScriptFile();

    File getAlteredVersionsFile();

    File getLockFile();

    File getDependenciesDir();

    String getDependenciesUser();
}