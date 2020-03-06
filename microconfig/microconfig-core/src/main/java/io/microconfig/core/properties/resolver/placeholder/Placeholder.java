package io.microconfig.core.properties.resolver.placeholder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static lombok.AccessLevel.PACKAGE;

@Getter
@RequiredArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder {
    private static final String SELF_REFERENCE = "this";

    private final Optional<String> configType;
    private final String component;
    private final String environment;
    private final String value;
    private final Optional<String> defaultValue;

    public Placeholder changeComponent(String component) {
        return changeComponentAndEnv(component, environment);
    }

    public Placeholder changeComponentAndEnv(String component, String environment) {
        return new Placeholder(configType, component, environment, value, defaultValue);
    }

    public boolean isSelfReferenced() {
        return SELF_REFERENCE.equals(component);
    }

    @Override
    public String toString() {
        return "${" + component + "[" + environment + "]@" + value + (defaultValue.map(v -> ":" + v)).orElse("") + "}";
    }
}