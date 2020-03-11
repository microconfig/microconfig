package io.microconfig.utils;

import static java.lang.System.getProperty;

public class OsUtil {
    private static final boolean windows = getProperty("os.name", "").startsWith("Win");

    public static boolean isWindows() {
        return windows;
    }
}
