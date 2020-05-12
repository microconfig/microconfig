package io.microconfig.utils;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.of;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
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

    public static Map<String, String> splitKeyValue(String... keyValue) {
        return Stream.of(keyValue)
                .map(s -> s.split("="))
                .collect(toLinkedMap(s -> s[0], s -> s.length == 1 ? "" : s[1]));
    }

    public static int findFirstIndexIn(String keyValue, String chars) {
        return range(0, keyValue.length())
                .filter(i -> {
                    char c = keyValue.charAt(i);
                    return chars.chars().anyMatch(ch -> ch == c);
                }).findFirst()
                .orElse(-1);
    }

    public static String addOffsets(String value, int spacesCount) {
        StringBuilder result = new StringBuilder(value);
        for (int i = 0; i < spacesCount; i++) {
            result.append(' ');
        }
        return result.toString();
    }

    public static long dotCountIn(String line) {
        return symbolCountIn(line, '.');
    }

    public static long symbolCountIn(String line, char symbol) {
        return line.chars().filter(c -> c == symbol).count();
    }

    public static String unixLikePath(String path) {
        return path.replace('\\', '/');
    }

    public static String toUnixPathSeparator(String line) {
        return line.replace("\r\n", "\n").trim();
    }

    public static String escape(String value) {
        String one = "\\";
        String two = "\\\\";
        return value.replace(two, one).replace(one, two);
    }

    public static StringBuilder asStringBuilder(CharSequence line) {
        return line instanceof StringBuilder ? (StringBuilder) line : new StringBuilder(line);
    }

    public static String getCauseMessage(Throwable t) {
        return getExceptionMessage(t.getCause());
    }

    public static String getExceptionMessage(Throwable t) {
        return ofNullable(t)
                .map(throwable -> "Cause: " + throwable.getMessage())
                .orElse("");
    }
}