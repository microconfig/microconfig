package deployment.mgmt.configs.updateconfigs;

import static java.util.Arrays.stream;

public enum UpdateConfigOption {
    CLEAN_ALTERED_VERSIONS,
    SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT;

    public static boolean isPresent(UpdateConfigOption option, UpdateConfigOption... options) {
        return stream(options).anyMatch(o -> o == option);
    }
}