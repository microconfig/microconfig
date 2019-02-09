package deployment.mgmt.init;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.fetch.ConfigFetcher;
import deployment.mgmt.configs.updateconfigs.UpdateConfigCommand;
import deployment.mgmt.configs.updateconfigs.UpdateConfigOption;
import deployment.mgmt.update.updater.MgmtAutoUpdater;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InitConfigsCommandImpl implements InitConfigsCommand {
    private final ConfigFetcher configFetcher;
    private final ComponentGroupService componentGroupService;
    private final UpdateConfigCommand updateConfigCommand;
    private final MgmtAutoUpdater mgmtAutoUpdater;

    @Override
    public void initConfigs(String configVersion, String projectFullVersionOrPostfix,
                            Runnable configFetchCallback,
                            UpdateConfigOption... options) {
        configFetcher.fetchConfigs(configVersion);
        configFetchCallback.run();

        componentGroupService.cleanAlteredVersions();
        componentGroupService.updateConfigVersion(configVersion);
        componentGroupService.updateProjectVersion(projectFullVersionOrPostfix);

        mgmtAutoUpdater.updateAndRestart("build-configs");
        updateConfigCommand.buildConfig(options);
    }
}