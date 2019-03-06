package io.microconfig.configs.provider;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * supported format #include component[optionalEnv]
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class Include {
    private static Pattern PATTERN = Pattern.compile("(?<comp>[\\w\\d\\s_-]+)(\\[(?<env>.+)])?");
    private static final String INCLUDE = "#include";
    private static final String INCLUDE2 = "#@include";

    private final String component;
    private final String env;

    static boolean isInclude(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith(INCLUDE) || lower.startsWith(INCLUDE2);
    }

    public static List<Include> parse(String line, String defaultEnv) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            return stream(parts, 1, parts.length)
                    .map(s -> s.replace(",", ""))
                    .map(String::trim)
                    .map(comp -> parseComponent(comp, defaultEnv))
                    .collect(toList());
        }

        throw new IllegalArgumentException("Can't parse include directive: " + line
                + ". Supported format: #include component[optionalEnv], component2[optionalEnv]"
        );
    }

    private static Include parseComponent(String compLine, String defaultEnv) {
        Matcher matcher = PATTERN.matcher(compLine);
        if (!matcher.find()) {
            throw new IllegalArgumentException("can't parse include component:" + compLine);
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