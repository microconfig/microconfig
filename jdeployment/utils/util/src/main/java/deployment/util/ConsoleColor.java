package deployment.util;

public class ConsoleColor {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";

    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";

    public static String green(String message) {
        return applyColor(message, GREEN);
    }

    public static String red(String message) {
        return applyColor(message, RED);
    }

    public static String yellow(String message) {
        return applyColor(message, YELLOW);
    }

    static String applyColor(String message, String color) {
        return message.isEmpty() ? "" : color + message + RESET;
    }
}