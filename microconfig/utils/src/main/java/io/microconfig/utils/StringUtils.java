package io.microconfig.utils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.of;

public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static List<String> split(String value, String separator) {
        return isEmpty(value) ? emptyList() :
                of(value.split(separator))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(toList());
    }

    public static String addOffsets(String value, int spacesCount) {
        StringBuilder result = new StringBuilder(value);
        for (int i = 0; i < spacesCount; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    public static int findFirstIndexIn(String keyValue, String chars) {
        return range(0, keyValue.length())
                .filter(i -> {
                    char c = keyValue.charAt(i);
                    return chars.chars().anyMatch(ch -> ch == c);
                }).findFirst()
                .orElse(-1);
    }

    public static String unixLikePath(String path) {
        return path.replace('\\', '/');
    }

    public static String escape(String value) {
        String one = "\\";
        String two = "\\\\";
        return value.replace(two, one).replace(one, two);
    }

    public static long symbolCountIn(String line, char symbol) {
        return line.chars().filter(c -> c == symbol).count();
    }
}