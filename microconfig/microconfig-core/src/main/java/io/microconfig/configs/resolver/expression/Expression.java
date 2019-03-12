package io.microconfig.configs.resolver.expression;

import io.microconfig.configs.resolver.PropertyResolveException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents Spring EL. Supported format #{expression}
 * <p>
 * Examples:
 * #{1+2} resolves to 3.
 * #{th@prop1 + th@prop2} sum value of this properties
 */
@Getter
@RequiredArgsConstructor
public class Expression {
    private static final ExpressionParser parser = new SpelExpressionParser();
    final static Pattern PATTERN = Pattern.compile("#\\{(?<value>[^{]+?)}");

    private final String value;

    public static Expression parse(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if (matcher.find()) return new Expression(matcher.group("value"));

        throw PropertyResolveException.badSpellFormat(value);
    }

    public String evaluate() {
        return parser.parseExpression(value).getValue(String.class);
    }

    @Override
    public String toString() {
        return "#{" + value + "}";
    }
}