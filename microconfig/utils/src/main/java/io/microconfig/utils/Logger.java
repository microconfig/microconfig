package io.microconfig.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static io.microconfig.utils.ConsoleColor.*;

public class Logger {
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

    public static void error(String message) {
        info(red(message));
        errorOccurred = true;
    }

    public static void warn(String message) {
        info(yellow(message));
    }

    public static void announce(String message) {
        info(green(message));
    }

    public static void info(String message) {
        System.out.println(message);
    }

    public static boolean isErrorOccurred() {
        return errorOccurred;
    }
}