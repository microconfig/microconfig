package deployment.mgmt.init;

import static io.microconfig.utils.SystemPropertiesUtils.getRequiredProperty;

public class InitParams {
    public static final String ENV = "env";
    public static final String GROUP = "group";
    public static final String CONFIG_GIT_URL = "configGitUrl";
    public static final String CONFIG_BRANCH_OR_TAG = "configBranchOrTag";
    public static final String PROJECT_FULL_VERSION_OR_POSTFIX = "projectFullVersionOrPostfix";
    public static final String NEXUS_CREDENTIALS = "nexusCredentials";

    public static final String CONFIG_SOURCE = "configSource";
    public static final String NEXUS_RELEASE_REPOSITORY = "nexusReleaseRepository";

    public static void verifyRequiredProperty() {
        getRequiredProperty(ENV);
        getRequiredProperty(GROUP);
        getRequiredProperty(CONFIG_GIT_URL);
        getRequiredProperty(CONFIG_BRANCH_OR_TAG);
        getRequiredProperty(PROJECT_FULL_VERSION_OR_POSTFIX);
        getRequiredProperty(NEXUS_CREDENTIALS);
        getRequiredProperty(CONFIG_SOURCE);
        getRequiredProperty(NEXUS_RELEASE_REPOSITORY);
    }

    public static String getConfigBranchOrTag() {
        return getRequiredProperty(CONFIG_BRANCH_OR_TAG);
    }

    public static String getProjectFullVersionOrPostfix() {
        return getRequiredProperty(PROJECT_FULL_VERSION_OR_POSTFIX);
    }

    public static String getEnv() {
        return getRequiredProperty(ENV);
    }

    public static String getGroup() {
        return getRequiredProperty(GROUP);
    }
}