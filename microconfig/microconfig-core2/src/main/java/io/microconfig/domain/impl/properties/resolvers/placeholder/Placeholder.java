package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Resolver.Expression;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static lombok.AccessLevel.PACKAGE;

@RequiredArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder implements Expression {
    private static final String SELF_REFERENCE = "this";

    @Getter
    private final int startIndex;
    @Getter
    private final int endIndex;

    private final Optional<String> configType;
    private final String component;
    private final String environment;
    private final String value;
    private final Optional<String> defaultValue;

    @Override
    public String resolve() {
        return null;
    }

    @Override
    public String toString() {
        return "${" +
                configType.map(t -> t + "::").orElse("") +
                component +
                "[" + environment + "]" +
                "@" +
                value +
                (defaultValue.map(v -> ":" + v)).orElse("") +
                "}";
    }
}