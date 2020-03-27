package io.microconfig.utils;

import static java.lang.System.getProperty;

public class Os {
    private static final boolean windows = getProperty("os.name", "").startsWith("Win");

    public static boolean isWindows() {
        return windows;
    }
}
