package deployment.mgmt.api.console;

import deployment.mgmt.lock.LockService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MgmtLockDecorator implements Mgmt {
    private final Mgmt mgmt;
    private final LockService lockService;

    @Override
    public void restart(String... services) {
        lockService.lockAndExecute(() -> mgmt.restart(services));
    }

    @Override
    public void restartWithArgs(String[] services, String... args) {
        lockService.lockAndExecute(() -> mgmt.restartWithArgs(services, args));
    }

    @Override
    public void restartGroup(String... groups) {
        lockService.lockAndExecute(() -> mgmt.restartGroup(groups));
    }

    @Override
    public void init(String configVersion, String projectFullVersionOrPostfix) {
        lockService.lockAndExecute(() -> mgmt.init(configVersion, projectFullVersionOrPostfix));
    }

    @Override
    public void init(String configVersion) {
        lockService.lockAndExecute(() -> mgmt.init(configVersion));
    }

    @Override
    public void fullInit() {
        lockService.lockAndExecute(mgmt::fullInit);
    }

    @Override
    public void smartDeploy(String configVersion, String projectFullVersionOrPostfix) {
        lockService.lockAndExecute(() -> mgmt.smartDeploy(configVersion, projectFullVersionOrPostfix));
    }

    @Override
    public void smartDeploy(String configVersion) {
        lockService.lockAndExecute(() -> mgmt.smartDeploy(configVersion));
    }

    @Override
    public void smartDeploy() {
        lockService.lockAndExecute(mgmt::smartDeploy);
    }

    @Override
    public void encryptProperties() {
        lockService.lockAndExecute(mgmt::encryptProperties);
    }

    @Override
    public void changeServiceVersion(String service, String serviceFullVersion) {
        lockService.lockAndExecute(() -> mgmt.changeServiceVersion(service, serviceFullVersion));
    }

    @Override
    public void stop(String... services) {
        lockService.lockAndExecute(() -> mgmt.stop(services));
    }

    @Override
    public void update(String minVersion) {
        lockService.lockAndExecute(() -> mgmt.update(minVersion));
    }

    @Override
    public void resetConfigs() {
        lockService.lockAndExecute(mgmt::resetConfigs);
    }
    //

    @Override
    public void updateOnEveryNode(String minVersion) {
        mgmt.updateOnEveryNode(minVersion);
    }
    @Override
    public void fetchConfigs() {
        mgmt.fetchConfigs();
    }

    @Override
    public void updateConfig() {
        mgmt.updateConfig();
    }

    @Override
    public void buildConfigs() {
        mgmt.buildConfigs();
    }
    //

    @Override
    public void status(String... services) {
        mgmt.status(services);
    }

    @Override
    public void groups() {
        mgmt.groups();
    }

    @Override
    public void killAllJava() {
        mgmt.killAllJava();
    }

    @Override
    public void log(String service, String logFileName) {
        mgmt.log(service, logFileName);
    }

    @Override
    public void log(String service) {
        mgmt.log(service);
    }

    @Override
    public void propertiesDiff(String... services) {
        mgmt.propertiesDiff(services);
    }

    @Override
    public void classpathDiff(String... services) {
        mgmt.classpathDiff(services);
    }

    @Override
    public void properties(String name) {
        mgmt.properties(name);
    }

    @Override
    public void newReleases(String... services) {
        mgmt.newReleases(services);
    }

    @Override
    public void ssh(String env, String group) {
        mgmt.ssh(env, group);
    }

    @Override
    public void ssh(String group) {
        mgmt.ssh(group);
    }

    @Override
    public void executeRemotely(String envGroup, String... command) {
        mgmt.executeRemotely(envGroup, command);
    }

    @Override
    public void memoryUsage() {
        mgmt.memoryUsage();
    }

    @Override
    public void setConfigGitUrl(String configGitUrl) {
        mgmt.setConfigGitUrl(configGitUrl);
    }

    @Override
    public void setNexusCredential(String credentials) {
        mgmt.setNexusCredential(credentials);
    }

    @Override
    public void setConfigSource(String configSource) {
        mgmt.setConfigSource(configSource);
    }

    @Override
    public void strictMode(boolean enable) {
        mgmt.strictMode(enable);
    }

    @Override
    public void version() {
        mgmt.version();
    }
}