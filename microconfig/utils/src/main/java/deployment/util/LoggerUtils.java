package deployment.util;

import static java.lang.Math.max;

public class LoggerUtils {
    private static int maxOneLineLength;

    public static synchronized void oneLineInfo(String message) {
        int initialMessageLength = message.length();

        maxOneLineLength = max(initialMessageLength, maxOneLineLength);
        if (maxOneLineLength > initialMessageLength) {
            message += String.format("%1$-" + (maxOneLineLength - initialMessageLength) + "s", "");
        }
        System.out.print("\r" + message);
        maxOneLineLength = Math.max(77, initialMessageLength);
    }
}