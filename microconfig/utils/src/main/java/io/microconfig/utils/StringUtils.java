package io.microconfig.utils;

import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Character.*;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String unixLikePath(String path) {
        return path.replace('\\', '/');
    }

    public static String replaceMultipleSpaces(String line) {
        return line.replaceAll("\\s+", " ").trim();
    }

    public static boolean like(String value, String like) {
        if (value == null || like == null) return false;

        String pattern = Pattern.quote(like)
                .replace("_", "\\E.\\Q")
                .replace("%", "\\E.*\\Q");
        return value.matches(pattern);
    }

    public static List<String> splitToList(String value, String separator) {
        return isEmpty(value) ? emptyList() :
                of(value.split(separator))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(toList());
    }

    public static String toLowerHyphen(String name) {
        return name.codePoints()
                .mapToObj(c -> isUpperCase((char) c) ? "-" + toLowerCase((char) c) : Character.toString((char) c))
                .collect(joining());
    }

    public static int indexOfFirstDigitOr(String line, int defaultIndex) {
        for (int i = 0; i < line.length(); i++) {
            if (isDigit(line.charAt(i))) return i;
        }
        return defaultIndex;
    }

    public static String addOffsets(String value, int spacesCount) {
        StringBuilder result = new StringBuilder(value);
        for (int i = 0; i < spacesCount; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    public static long symbolCountIn(String line, char symbol) {
        return line.chars().filter(c -> c == symbol).count();
    }
}