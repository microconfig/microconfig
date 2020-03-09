package io.microconfig.core.properties.provider;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

/**
 * supported format #include component[optionalEnv]
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class Include {
    private static Pattern COMPONENT_PATTERN = compile("^(?<comp>[\\w-_.]+)(\\[(?<env>.+)])?$");
    private static final String PREFIX = "#include";
    private static final String PREFIX2 = "#@include";

    private final String component;
    private final String env;

    //must be public for plugin
    public static boolean isInclude(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith(PREFIX) || lower.startsWith(PREFIX2);
    }

    public static List<Include> parse(String line, String defaultEnv) {
        try {
            return parseIncludes(line, defaultEnv);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Can't parse include '" + line + "'. " +
                    "Supported format: #include component[optionalEnv], component2[optionalEnv]", e);
        }
    }

    private static List<Include> parseIncludes(String line, String defaultEnv) {
        IntSupplier componentStartIndex = () -> {
            String lower = line.toLowerCase();
            {
                int index = tryPrefix(lower, PREFIX);
                if (index >= 0) return index;
            }
            {
                int index = tryPrefix(lower, PREFIX2);
                if (index >= 0) return index;
            }

            throw new IllegalArgumentException("Include must start with " + PREFIX + " or " + PREFIX2);
        };

        String[] components = line.substring(componentStartIndex.getAsInt()).split(",");
        if (components.length == 0) {
            throw new IllegalArgumentException("Include must contain component names");
        }

        return stream(components)
                .map(String::trim)
                .map(comp -> parseComponent(comp, defaultEnv))
                .collect(toList());
    }

    private static int tryPrefix(String value, String prefix) {
        int start = value.indexOf(prefix);
        return start >= 0 ? start + prefix.length() + 1 : -1;
    }

    private static Include parseComponent(String compLine, String defaultEnv) {
        Matcher matcher = COMPONENT_PATTERN.matcher(compLine);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Can't parse include's component: " + compLine);
        }

        String comp = matcher.group("comp");
        String env = matcher.group("env");
        return new Include(comp, env == null ? defaultEnv : env);
    }

    @Override
    public String toString() {
        return "#include " + component + "[" + env + "]";
    }
}