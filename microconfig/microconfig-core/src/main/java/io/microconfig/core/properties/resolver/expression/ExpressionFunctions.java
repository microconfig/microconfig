package io.microconfig.core.properties.resolver.expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.min;

public class ExpressionFunctions {
    public static String findGroup(String regex, String line) {
        Matcher matcher = Pattern.compile(regex).matcher(line);
        return matcher.find() ? matcher.group(min(1, matcher.groupCount())) : "";
    }
}
