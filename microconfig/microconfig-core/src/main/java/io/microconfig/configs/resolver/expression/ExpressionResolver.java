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
        String resolvedPlaceholders = delegate.resolve(property, root);
        return resolveSpels(resolvedPlaceholders, root);
    }

    private String resolveSpels(String placeholders, EnvComponent root) {
        StringBuilder currentValue = new StringBuilder(placeholders);
        while (true) {
            Matcher matcher = Expression.PATTERN.matcher(currentValue.toString());
            if (!matcher.find()) break;

            String resolvedValue = doResolve(matcher.group(), root);
            currentValue.replace(matcher.start(), matcher.end(), resolvedValue);
        }

        return currentValue.toString();
    }

    private String doResolve(String value, EnvComponent root) {
        Expression expression = Expression.parse(value);
        try {
            return expression.evaluate();
        } catch (RuntimeException e) {
            throw new PropertyResolveException(expression, root, e);
        }
    }
}