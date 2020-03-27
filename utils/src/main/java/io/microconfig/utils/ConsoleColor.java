package io.microconfig.utils;

public class ConsoleColor {
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RESET = "\u001B[0m";

    public static String green(String message) {
        return applyColor(message, GREEN);
    }

    public static String red(String message) {
        return applyColor(message, RED);
    }

    public static String yellow(String message) {
        return applyColor(message, YELLOW);
    }

    private static String applyColor(String message, String color) {
        return message.isEmpty() ? "" : color + message + RESET;
    }
}