package io.microconfig.core.properties.repository;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class Include {
    private static Pattern componentPattern = compile("^(?<comp>[\\w-_.]+)(\\[(?<env>.+)])?$");

    private final String component;
    private final String environment;

    public static Include parse(String definition, String defaultEnv) {
        Matcher componentMatcher = componentPattern.matcher(definition);
        if (!componentMatcher.find()) {
            throw new IllegalArgumentException("Can't parse include's component: " + definition);
        }

        String comp = componentMatcher.group("comp");
        String env = componentMatcher.group("env");
        return new Include(comp, env == null ? defaultEnv : env);
    }

    @Override
    public String toString() {
        return "#include " + component + "[" + environment + "]";
    }
}