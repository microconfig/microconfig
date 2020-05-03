package io.microconfig.core.properties.resolvers.expression.functions;

public class StringApi {
    public static String substring(String line, int beginIndex) {
        return line.substring(beginIndex);
    }

    public static String substring(String line, int beginIndex, int endIndex) {
        return line.substring(beginIndex, endIndex);
    }

    public static int indexOf(String line, String substring) {
        return line.indexOf(substring);
    }

    public static int indexOf(String line, String substring, int beginIndex) {
        return line.indexOf(substring, beginIndex);
    }

    public static int lastIndexOf(String line, String substring) {
        return line.lastIndexOf(substring);
    }

    public static int lastIndexOf(String line, String substring, int fromIndex) {
        return line.lastIndexOf(substring, fromIndex);
    }

    public static String toUpperCase(String line) {
        return line.toUpperCase();
    }

    public static String toLoweCase(String line) {
        return line.toLowerCase();
    }

    public static int length(String line) {
        return line.length();
    }

    public static boolean startsWith(String line, String substring) {
        return line.startsWith(substring);
    }

    public static boolean endsWith(String line, String substring) {
        return line.endsWith(substring);
    }

    public static char chartAt(String line, int index) {
        return line.charAt(index);
    }

    public static boolean matcher(String line, String regex) {
        return line.matches(regex);
    }

    public static boolean equalsIgnoreCase(String line, String line2) {
        return line.equalsIgnoreCase(line2);
    }

    public static String trim(String line) {
        return line.trim();
    }

    public static String replace(String line, CharSequence target, CharSequence replacement) {
        return line.replace(target, replacement);
    }

    public static String replaceAll(String line, String regex, String replacement) {
        return line.replaceAll(regex, replacement);
    }

    public static String delete(String line, CharSequence target) {
        return line.replace(target, "");
    }

    public static String[] split(String line, String regex) {
        return line.split(regex);
    }
}