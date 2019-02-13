package io.microconfig.utils;

import java.util.Map;
import java.util.function.UnaryOperator;

import static java.io.File.pathSeparator;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class OsUtil {
    private static final boolean windows = getProperty("os.name", "").startsWith("Win");

    public static boolean isWindows() {
        return windows;
    }

    public static String currentUser() {
        return System.getProperty("user.name");
    }
}
