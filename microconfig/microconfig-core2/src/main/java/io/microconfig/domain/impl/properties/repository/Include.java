package io.microconfig.domain.impl.properties.repository;

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

    private final String componentType;
    private final String environment;

    static Include parse(String component, String defaultEnv) {
        Matcher componentMatcher = componentPattern.matcher(component);
        if (!componentMatcher.find()) {
            throw new IllegalArgumentException("Can't parse include's component: " + component);
        }

        String comp = componentMatcher.group("comp");
        String env = componentMatcher.group("env");
        return new Include(comp, env == null ? defaultEnv : env);
    }

    @Override
    public String toString() {
        return "#include " + componentType + "[" + environment + "]";
    }
}