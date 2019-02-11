package io.microconfig.properties.resolver.spel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents Spring EL. Supported format #{expression}
 * <p>
 * examples:
 * #{1+2} resolves to 3.
 * #{th@prop1 + th@prop2} sum value of this properties
 */
@Getter
@RequiredArgsConstructor
public class SpelExpression {
    final static Pattern PATTERN = Pattern.compile("#\\{(?<value>[^{]+?)}");
    private final String value;

    public static SpelExpression parse(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.find()) {
            throw new IllegalArgumentException(value + " is not spel expression. Supported format is: #{expression}");
        }
        return new SpelExpression(matcher.group("value"));
    }

    @Override
    public String toString() {
        return "#{" + value + "}";
    }
}