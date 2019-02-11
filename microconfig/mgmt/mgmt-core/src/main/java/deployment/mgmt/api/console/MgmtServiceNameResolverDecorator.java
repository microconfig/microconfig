package deployment.mgmt.api.console;

import deployment.mgmt.configs.servicenameresolver.ServiceNameResolver;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class MgmtServiceNameResolverDecorator implements Mgmt {
    private final Mgmt delegate;
    private final ServiceNameResolver resolver;

    @Override
    public void status(String... services) {
        delegate.status(resolver.notStrictResolve(services));
    }

    @Override
    public void restart(String... services) {
        Function<Boolean, String[]> split = isArgs -> of(services).filter(s -> s.startsWith("-") == isArgs).toArray(String[]::new);
        String[] names = split.apply(false);
        String[] args = split.apply(true);

        restartWithArgs(names, args);
    }

    @Override
    public void restartWithArgs(String[] services, String[] args) {
        delegate.restartWithArgs(resolver.resolveWithoutTasks(services), args);
    }

    @Override
    public void stop(String... services) {
        delegate.stop(resolver.resolve(services));
    }

    @Override
    public void changeServiceVersion(String service, String serviceFullVersion) {
        resolver.requireCorrectName(service);
        delegate.changeServiceVersion(service, serviceFullVersion);
    }

    @Override
    public void log(String service, String logFileName) {
        delegate.log(resolver.resolveOne(service), logFileName);
    }

    @Override
    public void propertiesDiff(String... services) {
        delegate.propertiesDiff(resolver.notStrictResolve(services));
    }

    @Override
    public void classpathDiff(String... services) {
        delegate.classpathDiff(resolver.notStrictResolve(services));
    }

    @Override
    public void restartGroup(String... groups) {
        delegate.restartGroup(groups);
    }

    @Override
    public void init(String configVersion, String projectFullVersionOrPostfix) {
        delegate.init(configVersion, projectFullVersionOrPostfix);
    }

    @Override
    public void smartDeploy() {
        delegate.smartDeploy();
    }

    @Override
    public void smartDeploy(String configVersion, String projectFullVersionOrPostfix) {
        delegate.smartDeploy(configVersion, projectFullVersionOrPostfix);
    }

    @Override
    public void fetchConfigs() {
        delegate.fetchConfigs();
    }

    @Override
    public void buildConfigs() {
        delegate.buildConfigs();
    }

    @Override
    public void groups() {
        delegate.groups();
    }

    @Override
    public void resetConfigs() {
        delegate.resetConfigs();
    }

    @Override
    public void encryptProperties() {
        delegate.encryptProperties();
    }

    @Override
    public void properties(String name) {
        delegate.properties(name);
    }

    @Override
    public void killAllJava() {
        delegate.killAllJava();
    }

    @Override
    public void strictMode(boolean enable) {
        delegate.strictMode(enable);
    }

    @Override
    public void update(String minVersion) {
        delegate.update(minVersion);
    }

    @Override
    public void updateOnEveryNode(String minVersion) {
        delegate.updateOnEveryNode(minVersion);
    }

    @Override
    public void version() {
        delegate.version();
    }

    @Override
    public void memoryUsage() {
        delegate.memoryUsage();
    }

    @Override
    public void newReleases(String... services) {
        delegate.newReleases(resolver.resolve(services));
    }

    @Override
    public void fullInit() {
        delegate.fullInit();
    }

    @Override
    public void setConfigGitUrl(String configGitUrl) {
        delegate.setConfigGitUrl(configGitUrl);
    }

    @Override
    public void setNexusCredential(String credentials) {
        delegate.setNexusCredential(credentials);
    }

    @Override
    public void setConfigSource(String configSource) {
        delegate.setConfigSource(configSource);
    }

    @Override
    public void ssh(String group) {
        delegate.ssh(group);
    }

    @Override
    public void executeRemotely(String envGroup, String... command) {
        delegate.executeRemotely(envGroup, command);
    }

    @Override
    public void ssh(String env, String group) {
        delegate.ssh(env, group);
    }
}