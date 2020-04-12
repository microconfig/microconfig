package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import static java.lang.System.getProperty;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class Os {
    private static final boolean windows = getProperty("os.name", "").startsWith("Win");

    public static boolean isWindows() {
        return windows;
    }
}
