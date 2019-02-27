package io.microconfig.configs.provider;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * supported format #include component[optionalEnv]
 */
@EqualsAndHashCode(of = {"component", "env"})
public class Include {
    final static Pattern PATTERN = Pattern.compile("[#@][iI]nclude\\s+(?<comp>[\\w\\d\\s_-]+)(\\[(?<env>.+)])?");

    @Getter
    private final String component;
    @Getter
    private final String env;

    static boolean isInclude(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("#include") || lower.startsWith("#@include");
    }

    public static Include parse(String line, String defaultEnv) {
        return new Include(line, defaultEnv);
    }

    private Include(String line, String defaultEnv) {
        Matcher matcher = PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Can't parse include directive: " + line + ". Supported format: #include component[optionalEnv]");
        }

        this.component = requireNonNull(matcher.group("comp")).trim();
        this.env = requireNonNull(ofNullable(matcher.group("env")).orElse(defaultEnv)).trim();
    }

    @Override
    public String toString() {
        return "#include " + component + "[" + env + "]";
    }
}