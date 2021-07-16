package io.microconfig.core.properties.resolvers.expression.functions;

import java.io.File;
import java.util.regex.Matcher;

import static io.microconfig.utils.IoUtils.readAllBytes;
import static io.microconfig.utils.IoUtils.readFully;
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
        return base64(line.getBytes());
    }

    public static String base64(byte[] bytes) {
        return getEncoder().encodeToString(bytes);
    }

    public static String delete(String line, String toDelete) {
        return line.replace(toDelete, "");
    }

    public static String substringAfterFirst(String line, String substring) {
        int i = line.indexOf(substring);
        return substringAfter(line, i, substring);
    }

    public static String substringAfterLast(String line, String substring) {
        int i = line.lastIndexOf(substring);
        return substringAfter(line, i, substring);
    }

    private static String substringAfter(String line, int i, String substring) {
        if (i < 0) return "";
        return line.substring(i + substring.length());
    }

    public static byte[] readBytes(String path) {
        return readAllBytes(new File(path));
    }

    public static byte[] readBytesOrEmpty(String path) {
        File file = new File(path);
        if (!file.exists()) return new byte[0];
        return readAllBytes(file);
    }

    public static String readString(String path) {
        return readFully(new File(path));
    }

    public static String readStringOrEmpty(String path) {
        File file = new File(path);
        if (!file.exists()) return "";
        return readFully(file);
    }
}