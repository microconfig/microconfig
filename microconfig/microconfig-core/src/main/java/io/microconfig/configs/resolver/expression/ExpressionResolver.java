package io.microconfig.configs.resolver.expression;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolveException;
import io.microconfig.configs.resolver.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;

/**
 * Resolves placeholders in format of spring EL
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
        StringBuilder currentValue = new StringBuilder(value);

        while (true) {
            Matcher matcher = Expression.matcher(currentValue.toString());
            if (!matcher.find()) break;

            String evaluatedValue = doEvaluate(matcher.group(), root);
            currentValue.replace(matcher.start(), matcher.end(), evaluatedValue);
        }

        return currentValue.toString();
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