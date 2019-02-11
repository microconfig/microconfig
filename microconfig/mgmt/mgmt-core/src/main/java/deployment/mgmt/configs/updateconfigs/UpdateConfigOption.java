package deployment.mgmt.configs.updateconfigs;

import java.util.Set;

public enum UpdateConfigOption {
    CLEAN_ALTERED_VERSIONS,
    SKIP_CLASSPATH_BUILD_FOR_SNAPSHOT;

    public static boolean in(UpdateConfigOption[] options, UpdateConfigOption option) {
        return Set.of(options).contains(option);
    }
}