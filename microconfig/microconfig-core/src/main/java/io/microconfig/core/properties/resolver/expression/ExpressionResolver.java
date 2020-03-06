package io.microconfig.core.properties.resolver.expression;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolveException;
import io.microconfig.core.properties.resolver.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;

/**
 * Resolves placeholders in format of Spring EL
 *
 * @see Expression
 */
@RequiredArgsConstructor
public class ExpressionResolver implements PropertyResolver {
    private final PropertyResolver delegate;

    @Override
    public String resolve(Property property, EnvComponent root) {
        String resolvedValue = delegate.resolve(property, root);
        return evaluate(resolvedValue, root);
    }

    private String evaluate(String value, EnvComponent root) {
        StringBuilder result = new StringBuilder(value);

        while (true) {
            Matcher matcher = Expression.matcher(result);
            if (!matcher.find()) break;

            String evaluated = doEvaluate(matcher.group(), root);
            result.replace(matcher.start(), matcher.end(), evaluated);
        }

        return result.toString();
    }

    private String doEvaluate(String value, EnvComponent root) {
        Expression expression = Expression.parse(value);

        try {
            return expression.evaluate();
        } catch (RuntimeException e) {
            throw new PropertyResolveException(expression, root, e);
        }
    }
}