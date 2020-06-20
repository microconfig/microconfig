package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;

import static io.microconfig.utils.ConsoleColor.*;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class Logger {
    private static volatile boolean enabled = true;
    private static volatile boolean errorOccurred;

    public static void error(Throwable e) {
        if (e != null) {
            error(null, e);
        }
    }

    public static void error(String message, Throwable e) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        if (message != null) {
            writer.println(message);
        }
        if (e != null) {
            e.printStackTrace(writer);
        }

        error(out.toString());
    }

    public static void warn(String message) {
        info(yellow(message));
    }

    public static void announce(String message) {
        info(green(message));
    }

    public static void info(String message) {
        if (!enabled) return;
        System.out.println(message);
    }

    public static void error(String message) {
        errorOccurred = true;
        System.out.println(red(message));
    }

    public static boolean isErrorOccurred() {
        return errorOccurred;
    }

    public static void enableLogger(boolean enabled) {
        Logger.enabled = enabled;
    }
}