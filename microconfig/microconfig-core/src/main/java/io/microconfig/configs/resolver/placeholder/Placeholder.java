package io.microconfig.configs.resolver.placeholder;

import io.microconfig.configs.resolver.PropertyResolveException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder {
    private static final String SELF_REFERENCE = "this";

    static final Pattern PLACEHOLDER_INSIDE_LINE = compile("\\$\\{(?<comp>[\\w\\-_:]+)(\\[(?<env>[\\w\\-_]+)])?@(?<value>[\\w\\-._]+)(:(?<default>[^$}]+))?}");
    static final Pattern SINGE_PLACEHOLDER = compile("^\\$\\{((?<type>\\w+)::)?(?<comp>[\\s\\w._-]+)(\\[(?<env>.+)])?@(?<value>[\\w._-]+)(:(?<default>.+))?}$");

    private final Optional<String> type;

    private final String component;
    private final String environment;
    private final String value;

    private final Optional<String> defaultValue;

    public static boolean isSinglePlaceholder(String value) {
        return SINGE_PLACEHOLDER.matcher(value).matches();
    }

    public static Matcher placeholderMatcher(CharSequence line) {
        return PLACEHOLDER_INSIDE_LINE.matcher(line);
    }

    public static Placeholder parse(String value, String defaultEnv) {
        return new Placeholder(value, defaultEnv);
    }

    private Placeholder(String value, String defaultEnv) {
        Matcher matcher = SINGE_PLACEHOLDER.matcher(value);
        if (!matcher.find()) {
            throw PropertyResolveException.badPlaceholderFormat(value);
        }

        this.type = ofNullable(matcher.group("type"));
        this.component = requireNonNull(matcher.group("comp"));
        this.environment = requireNonNull(ofNullable(matcher.group("env")).orElse(defaultEnv));
        this.value = requireNonNull(matcher.group("value"));
        this.defaultValue = ofNullable(matcher.group("default"));
    }

    public Placeholder changeComponent(String componentName) {
        return changeComponent(componentName, environment);
    }

    public Placeholder changeComponent(String component, String environment) {
        return new Placeholder(type, component, environment, value, defaultValue);
    }

    public boolean isSelfReferenced() {
        return SELF_REFERENCE.equals(component);
    }

    @Override
    public String toString() {
        return component + "[" + environment + "]@" + value;
    }
}