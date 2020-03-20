package io.microconfig.core.properties.resolver.expression;

import io.microconfig.core.properties.resolver.PropertyResolveException;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.core.properties.resolver.expression.ExpressionEvaluator.withPredefinedFunctionsFrom;
import static java.util.regex.Pattern.compile;

/**
 * Represents Spring EL. Supported format #{expression}
 * <p>
 * Examples:
 * #{1+2} resolves to 3.
 * #{th@prop1 + th@prop2} sum value of this properties
 */
@RequiredArgsConstructor
public class Expression {
    private static final Pattern PATTERN = compile("#\\{(?<value>[^{]+?)}");
    private static final ExpressionEvaluator evaluator = withPredefinedFunctionsFrom(ExpressionFunctions.class);

    private final String value;

    public static Expression parse(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if (matcher.find()) {
            return new Expression(matcher.group("value"));
        }

        throw PropertyResolveException.badSpellFormat(value);
    }

    static Matcher matcher(CharSequence value) {
        return PATTERN.matcher(value);
    }

    public String evaluate() {
        return evaluator.evaluate(value);
    }

    @Override
    public String toString() {
        return "#{" + value + "}";
    }
}