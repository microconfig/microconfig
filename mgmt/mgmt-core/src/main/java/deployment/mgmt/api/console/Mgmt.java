package deployment.mgmt.api.console;

import deployment.console.ConsoleOrder;
import deployment.console.ConsoleParam;
import deployment.console.Hidden;

import static deployment.mgmt.atrifacts.Artifact.SNAPSHOT;

public interface Mgmt {
    @ConsoleOrder(1)
    void status(@ConsoleParam("serviceNames") String... services);

    @ConsoleOrder(2)
    void restart(@ConsoleParam("serviceNames") String... services);

    @Hidden
    void restartWithArgs(String[] services, String... args);

    @ConsoleOrder(3)
    void restartGroup(@ConsoleParam("groups") String... groups);

    @ConsoleOrder(4)
    void groups();

    @ConsoleOrder(5)
    void stop(@ConsoleParam("serviceNames") String... services);

    @ConsoleOrder(6)
    void init(@ConsoleParam("configVersion") String configVersion,
              @ConsoleParam(value = "projectFullVersionOrPostfix", defaultValue = "-SNAPSHOT") String projectFullVersionOrPostfix);

    @Hidden
    default void init(String configVersion) {
        init(configVersion, SNAPSHOT);
    }

    @Hidden
    void fullInit();

    @ConsoleOrder(7)
    void smartDeploy(@ConsoleParam(value = "configVersion", defaultValue = "current") String configVersion,
                     @ConsoleParam(value = "projectFullVersionOrPostfix", defaultValue = "-SNAPSHOT") String projectFullVersionOrPostfix);

    @Hidden
    default void smartDeploy(String configVersion) {
        smartDeploy(configVersion, SNAPSHOT);
    }

    @Hidden
    void smartDeploy();

    @ConsoleOrder(8)
    void killAllJava();

    @ConsoleOrder(9)
    void changeServiceVersion(@ConsoleParam("service") String service, @ConsoleParam("serviceFullVersion") String serviceFullVersion);

    @ConsoleOrder(10)
    void log(@ConsoleParam("service") String service, @ConsoleParam(value = "logFileName", optional = true) String logFileName);

    @Hidden
    default void log(@ConsoleParam("service") String service) {
        log(service, null);
    }

    @ConsoleOrder(11)
    void propertiesDiff(@ConsoleParam("serviceNames") String... services);

    @ConsoleOrder(12)
    void classpathDiff(@ConsoleParam("serviceNames") String... services);

    @ConsoleOrder(13)
    void fetchConfigs();

    @ConsoleOrder(14)
    void buildConfigs();

    @ConsoleOrder(15)
    void resetConfigs();

    @ConsoleOrder(16)
    void properties(@ConsoleParam("propertyName") String name);

    @ConsoleOrder(17)
    void encryptProperties();

    @ConsoleOrder(18)
    void newReleases(@ConsoleParam("serviceNames") String... services);

    @ConsoleOrder(19)
    void ssh(@ConsoleParam(value = "env", defaultValue = "current") String env, @ConsoleParam("group") String group);

    @Hidden
    void ssh(String group);

    @ConsoleOrder(20)
    void executeRemotely(@ConsoleParam("env:group or group") String envGroup, @ConsoleParam("command") String... command);

    @ConsoleOrder(21)
    void memoryUsage();

    @ConsoleOrder(22)
    void setConfigGitUrl(@ConsoleParam("configGitUrl") String configGitUrl);

    @ConsoleOrder(23)
    void setNexusCredential(@ConsoleParam("login:password") String credentials);

    @ConsoleOrder(24)
    void setConfigSource(@ConsoleParam("git or nexus") String configSource);

    @ConsoleOrder(25)
    void strictMode(@ConsoleParam("true or false") boolean enable);

    @ConsoleOrder(26)
    void version();

    @ConsoleOrder(27)
    void update(@ConsoleParam("mgmtVersion") String minVersion);

    @ConsoleOrder(28)
    void updateOnEveryNode(@ConsoleParam("mgmtVersion") String minVersion);
}