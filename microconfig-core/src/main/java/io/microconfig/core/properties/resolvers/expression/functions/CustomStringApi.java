package io.microconfig.core.properties.resolvers.expression.functions;

import java.util.regex.Matcher;

import static java.lang.Math.min;
import static java.util.Base64.getEncoder;
import static java.util.regex.Pattern.compile;

public class CustomStringApi {
    public static String findGroup(String regex, String line) {
        return findGroupOrDefault(regex, line, "");
    }

    public static String findGroupOrDefault(String regex, String line, String defaultValue) {
        Matcher matcher = compile(regex).matcher(line);
        return matcher.find() ? matcher.group(min(1, matcher.groupCount())) : defaultValue;
    }

    public static String base64(String line) {
        return getEncoder().encodeToString(line.getBytes());
    }

    public static String delete(String line, String toDelete) {
        return line.replace(toDelete, "");
    }

    public static String substringAfterFirst(String line, String substring) {
        int i = line.indexOf(substring);
        if (i < 0) return "";
        return line.substring(i + substring.length());
    }

    public static String substringAfterLast(String line, String substring) {
        int i = line.lastIndexOf(substring);
        if (i < 0) return "";
        return line.substring(i + substring.length());
    }

    public static String substringBeforeFirst(String line, String substring) {
        int i = line.indexOf(substring);
        if (i < 0) return "";
        return line.substring(0, i);
    }
}