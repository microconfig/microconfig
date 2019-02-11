package io.microconfig.configs.properties.resolver.placeholder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

/**
 * supported format  ${componentName[optionalEnvName]@propertyPlaceholder:optionalDefaultValue}
 */
@Getter
@RequiredArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder {
    public static final Pattern PATTERN_FOR_RESOLVE = Pattern.compile("\\$\\{(?<comp>[\\w\\-_]+)(\\[(?<env>[\\w\\-_]+)])?@(?<value>[\\w\\-._]+)(:(?<default>[^$}]+))?}");
    static final Pattern PATTERN = Pattern.compile("\\$\\{(?<comp>[\\s\\w._-]+)(\\[(?<env>.+)])?@(?<value>[\\w._-]+)(:(?<default>.+))?}");

    private final String component;
    private final String environment;
    private final String value;
    private final Optional<String> defaultValue;

    public static Placeholder parse(String value, String defaultEnv) {
        return new Placeholder(value, defaultEnv);
    }

    private Placeholder(String value, String defaultEnv) {
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Can't parse placeholders: " + value + ". Supported format: ${componentName[optionalEnvName]@propertyPlaceholder:optionalDefaultValue}");
        }

        this.component = requireNonNull(matcher.group("comp"));
        this.environment = requireNonNull(ofNullable(matcher.group("env")).orElse(defaultEnv));
        this.value = requireNonNull(matcher.group("value"));
        this.defaultValue = ofNullable(matcher.group("default"));
    }

    public Placeholder changeComponent(String component, String environment) {
        return new Placeholder(component, environment, value, defaultValue);
    }

    public Placeholder changeValue(String value) {
        return new Placeholder(component, environment, value, defaultValue);
    }

    public Placeholder changeComponent(String componentName) {
        return changeComponent(componentName, environment);
    }

    @Override
    public String toString() {
        return component + "[" + environment + "]@" + value;
    }
}