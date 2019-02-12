package deployment.mgmt.configs.filestructure;

import java.io.File;
import java.util.function.Function;

import static io.microconfig.utils.FileUtils.createDir;
import static io.microconfig.utils.FileUtils.userHome;


public class ConfigDirsImpl implements ConfigDirs {
    @Override
    public File getConfigRepoRootDir() {
        return createDir(new File(userHome(), "config-repo"));
    }

    @Override
    public File getInnerRepoDir() {
        return createDir(new File(getConfigRepoRootDir(), "repo"));
    }

    @Override
    public File getConfigVersionFile() {
        return configFile("/components/system/versions/config-version.proc");
    }

    @Override
    public File getProjectVersionFile(String env) {
        return configFile("/components/system/versions/project-version" + (env == null ? "" : "." + env) + ".proc");
    }

    @Override
    public File getScriptsDir() {
        return new File(getConfigRepoRootDir(), "/bin/scripts");
    }

    @Override
    public File getMgmtScriptsDir() {
        return new File(getConfigRepoRootDir(), "/mgmt");
    }

    @Override
    public File getMgmtArtifactFile(String env) {
        Function<String, File> mgmtFile = suffix -> configFile("/components/mgmt/mgmt-version/mgmt" + suffix + ".proc");

        File envFile = mgmtFile.apply("." + env);
        return envFile.exists() ? envFile : mgmtFile.apply("");
    }

    @Override
    public File getEnvCfgFile() {
        return new File(getConfigRepoRootDir(), "share/env/env.cfg");
    }

    private File configFile(String file) {
        return new File(getInnerRepoDir(), file);
    }
}