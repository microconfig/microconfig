package io.microconfig.core.resolvers.expression;

import java.util.regex.Matcher;

import static java.lang.Math.min;
import static java.util.regex.Pattern.compile;

public class PredefinedFunctions {
    public static String findGroup(String regex, String line) {
        return findGroupOrDefault(regex, line, "");
    }

    public static String findGroupOrDefault(String regex, String line, String defaultValue) {
        Matcher matcher = compile(regex).matcher(line);
        return matcher.find() ? matcher.group(min(1, matcher.groupCount())) : defaultValue;
    }
}
