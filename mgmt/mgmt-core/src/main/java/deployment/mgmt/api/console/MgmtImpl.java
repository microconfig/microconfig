package deployment.mgmt.api.console;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.componentgroup.ServiceDescription;
import deployment.mgmt.configs.componentgroup.ServiceGroupManager;
import deployment.mgmt.configs.deploysettings.ConfigSource;
import deployment.mgmt.configs.deploysettings.DeploySettings;
import deployment.mgmt.configs.diff.ShowDiffCommand;
import deployment.mgmt.configs.encryption.EncryptPropertiesCommand;
import deployment.mgmt.configs.updateconfigs.UpdateConfigCommand;
import deployment.mgmt.init.InitService;
import deployment.mgmt.process.log.LogCommand;
import deployment.mgmt.process.start.StartCommand;
import deployment.mgmt.process.status.StatusCommand;
import deployment.mgmt.process.stop.StopService;
import deployment.mgmt.ssh.SshCommand;
import deployment.mgmt.stat.monitoring.MonitoringService;
import deployment.mgmt.stat.releases.ReadyReleasesService;
import deployment.mgmt.update.updater.MgmtAutoUpdater;
import io.microconfig.utils.Logger;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

import static deployment.mgmt.atrifacts.Artifact.SNAPSHOT;
import static deployment.mgmt.configs.service.properties.impl.StandardServiceGroup.*;
import static deployment.mgmt.configs.updateconfigs.UpdateConfigOption.CLEAN_ALTERED_VERSIONS;
import static deployment.mgmt.configs.updateconfigs.UpdateConfigOption.SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT;
import static deployment.mgmt.init.InitParams.getConfigBranchOrTag;
import static deployment.mgmt.init.InitParams.getProjectFullVersionOrPostfix;
import static deployment.mgmt.process.status.ExecutionStatus.RUNNING;
import static io.microconfig.utils.Logger.*;
import static io.microconfig.utils.SystemPropertiesUtils.hasSystemFlag;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.util.List.of;

//todo2 refactor
@RequiredArgsConstructor
public class MgmtImpl implements Mgmt {
    private final ComponentGroupService componentGroupService;
    private final StatusCommand statusCommand;
    private final ServiceGroupManager serviceGroupManager;
    private final StartCommand startCommand;
    private final StopService stopService;

    private final InitService initService;
    private final EncryptPropertiesCommand encryptPropertiesCommand;
    private final UpdateConfigCommand updateConfigCommand;
    private final ShowDiffCommand showDiffCommand;

    private final LogCommand logCommand;
    private final DeploySettings deploySettings;
    private final MgmtAutoUpdater mgmtAutoUpdater;
    private final ReadyReleasesService readyReleasesService;
    private final SshCommand sshCommand;
    private final MonitoringService monitoringService;

    @Override
    public void status(String... services) {
        statusCommand.printStatus(services);
    }

    @Override
    public void restart(String... services) {
        restartWithArgs(services);
    }

    @Override
    public void restartWithArgs(String[] services, String... args) {
        long t = currentTimeMillis();

        stopService.stop(services);
        logLineBreak();

        startCommand.startWithArgs(services, args);
        status(services);

        if (services.length > 1) {
            announce("\nStarted " + services.length + " services in " + secAfter(t));
        }
    }

    @Override
    public void restartGroup(String... groups) {
        List<String> services = serviceGroupManager.findServicesByGroup(groups);
        if (services.isEmpty()) {
            warn("Found 0 services with groups: " + Arrays.toString(groups));
            return;
        }

        restart(services.toArray(new String[0]));
    }

    @Override
    public void groups() {
        serviceGroupManager.printServiceGroups();
    }

    @Override
    public void stop(String... services) {
        stopService.stop(services);
        status(services);
    }

    @Override
    public void init(String configVersion, String projectFullVersionOrPostfix) {
        initService.init(configVersion, projectFullVersionOrPostfix);
    }

    @Override
    public void smartDeploy(String configVersion, String projectFullVersionOrPostfix) {
        announce("Smart deploy with configs: " + configVersion + ", project: " + projectFullVersionOrPostfix);
        long t = currentTimeMillis();

        mgmtAutoUpdater.registerRestartCommand("smart-deploy", configVersion, projectFullVersionOrPostfix);
        initService.initWithoutStopping(configVersion, projectFullVersionOrPostfix);
        smartRestart();

        announce("\nFinished smart deploy in " + secAfter(t));
    }

    @Override
    public void smartDeploy() {
        smartDeploy(deploySettings.getConfigVersion(), SNAPSHOT);
    }

    @Override
    public void fetchConfigs() {
        initService.fetchConfigs();
    }

    @Override
    public void changeServiceVersion(String service, String serviceFullVersion) {
        componentGroupService.changeServiceVersion(new ServiceDescription(service, serviceFullVersion));

        if (statusCommand.getStatus(service).getValue() != RUNNING) {
            componentGroupService.replaceServiceVersionWithAltered(service);
        }
    }

    @Override
    public void propertiesDiff(String... service) {
        showDiffCommand.showPropDiff(service);
    }

    @Override
    public void classpathDiff(String... services) {
        showDiffCommand.showClasspathDiff(services);
    }

    @Override
    public void log(String service, String logFileName) {
        logCommand.log(service, logFileName);
    }

    @Override
    public void buildConfigs() {
        updateConfigCommand.buildConfig(SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT);
    }

    @Override
    public void resetConfigs() {
        updateConfigCommand.buildConfig(CLEAN_ALTERED_VERSIONS);
    }

    @Override
    public void encryptProperties() {
        encryptPropertiesCommand.encryptSecretProperties();
    }

    @Override
    public void properties(String name) {
        showDiffCommand.printProperties(name);
    }

    @Override
    public void killAllJava() {
        stopService.killAllJava();
    }

    @Override
    public void strictMode(boolean enable) {
        deploySettings.strictMode(enable);
    }

    @Override
    public void update(String minVersion) {
        mgmtAutoUpdater.updateAndRestart(minVersion, true, "version");
        warn("Canceled updateSecrets, current version: " + deploySettings.getCurrentMgmtArtifact().getVersion());
    }

    @Override
    public void updateOnEveryNode(String minVersion) {
        sshCommand.executeOnEveryNode(componentGroupService.getEnv(), "mgmt updateSecrets " + minVersion,
                (group) -> info("Updating " + group.getName()),
                (group, output) -> {
                    if (output == null) {
                        error("Failed to updateSecrets on " + group.getName());
                    } else {
                        announce("Updated on " + group.getName());
                    }
                    return null;
                });
    }

    @Override
    public void memoryUsage() {
        monitoringService.getGroupMemoryUsage().outputTo(Logger::info);
    }

    @Override
    public void version() {
        announce("mgmt version: " + deploySettings.getCurrentMgmtArtifact().getVersion());
    }

    @Override
    public void newReleases(String... services) {
        readyReleasesService.serviceToNewReleases(of(services)).outputTo(Logger::info);
    }

    @Override
    public void fullInit() {
        if (hasSystemFlag("smartDeploy")) {
            mgmtAutoUpdater.registerRestartCommand(
                    "smart-deploy",
                    getConfigBranchOrTag(),
                    getProjectFullVersionOrPostfix()
            );
            initService.fullInit(true);

            announce("Smart restarting services");
            smartRestart();
        } else {
            initService.fullInit(false);
        }
    }

    @Override
    public void setConfigGitUrl(String configGitUrl) {
        deploySettings.setConfigGitUrl(configGitUrl);
    }

    @Override
    public void setNexusCredential(String credentials) {
        deploySettings.setNexusCredentials(credentials);
    }

    @Override
    public void setConfigSource(String configSource) {
        ConfigSource value;
        try {
            value = ConfigSource.valueOf(configSource.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Config source must be one of " + Arrays.toString(ConfigSource.values()));
        }

        deploySettings.setConfigSource(value);
    }

    @Override
    public void ssh(String group) {
        ssh(componentGroupService.getEnv(), group);
    }

    @Override
    public void executeRemotely(String envGroup, String... command) {
        String env;
        String group;
        String[] parts = envGroup.split(":");
        if (parts.length == 1) {
            env = componentGroupService.getEnv();
            group = parts[0];
        } else {
            env = parts[0];
            group = parts[1];
        }

        sshCommand.executeOn(env, group, join(" ", command));
    }

    @Override
    public void ssh(String env, String group) {
        sshCommand.ssh(env, group);
    }

    private void smartRestart() {
        restartGroup(STOPPED.groupName(), FAILED.groupName(), CHANGED.groupName()); //todo classpath diff will be removed during start phase
    }
}