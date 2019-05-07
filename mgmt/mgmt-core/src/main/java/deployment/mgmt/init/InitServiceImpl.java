package deployment.mgmt.init;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.componentgroup.GroupDescription;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.updateconfigs.UpdateConfigOption;
import deployment.mgmt.process.stop.StopService;
import deployment.mgmt.update.scriptgenerator.MgmtScriptGenerator;
import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;

import static deployment.mgmt.configs.updateconfigs.UpdateConfigOption.SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT;
import static deployment.mgmt.init.InitParams.*;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.System.currentTimeMillis;

@RequiredArgsConstructor
public class InitServiceImpl implements InitService {
    private final MgmtScriptGenerator mgmtScriptGenerator;
    private final StopService stopService;
    private final OldFilesCleaner oldFilesCleaner;
    private final DeploySettings deploySettings;
    private final ComponentGroupService componentGroupService;
    private final PwdService pwdService;

    private final EnvDependenciesDownloader envDependenciesDownloader;
    private final InitConfigsCommand initConfigsCommand;

    @Override
    public void fullInit(boolean withoutStopping) {
        InitParams.verifyRequiredProperty();

        Runnable saveGroupDescription = () -> componentGroupService.update(new GroupDescription(getEnv(), getGroup()));
        Runnable saveNexusInfo = () -> {
            deploySettings.getNexusCredentials();
            deploySettings.getNexusReleaseRepository();
            deploySettings.getConfigSource();
        };
        Runnable createDefaultPwdFile = () -> pwdService.createPwdFile(getEnv());
        Runnable generateMgmtScript = mgmtScriptGenerator::generateMgmtScript;

        saveGroupDescription.run();
        saveNexusInfo.run();
        createDefaultPwdFile.run();
        generateMgmtScript.run();

        BiConsumer<String, String> method = withoutStopping ? this::initWithoutStopping : this::init;
        method.accept(getConfigBranchOrTag(), getProjectFullVersionOrPostfix());
    }

    @Override
    public void init(String configVersion, String projectFullVersionOrPostfix) {
        if (projectFullVersionOrPostfix.equals(".")) {
            projectFullVersionOrPostfix = configVersion;
        }

        announce("Init with configs: " + configVersion + ", project: " + projectFullVersionOrPostfix);
        long t = currentTimeMillis();

        stopService.stopAll();
        oldFilesCleaner.deleteOldFiles();

        saveConfigVersion(configVersion);
        initConfigs(configVersion, projectFullVersionOrPostfix, SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT);

        announce("\nFinished init in " + secAfter(t));
    }

    @Override
    public void initWithoutStopping(String configVersion, String projectFullVersionOrPostfix) {
        saveConfigVersion(configVersion);
        initConfigs(configVersion, projectFullVersionOrPostfix);
    }

    @Override
    public void fetchConfigs() {
        initConfigs(deploySettings.getConfigVersion(), componentGroupService.getProjectVersion(), SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT);
    }

    private void initConfigs(String configVersion, String projectFullVersionOrPostfix, UpdateConfigOption... options) {
        Runnable configFetchCallback = () -> envDependenciesDownloader.downloadDependencies(componentGroupService.getEnv());

        initConfigsCommand.initConfigs(
                configVersion, projectFullVersionOrPostfix,
                configFetchCallback,
                options
        );
    }

    private void saveConfigVersion(String configVersion) {
        deploySettings.setConfigVersion(configVersion);
    }
}