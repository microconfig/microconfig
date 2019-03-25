package deployment.mgmt.init;

import deployment.mgmt.configs.updateconfigs.UpdateConfigOption;

public interface InitConfigsCommand {
    void initConfigs(String configVersion, String projectFullVersionOrPostfix,
                     Runnable postFetchCallback,
                     UpdateConfigOption... options);
}
