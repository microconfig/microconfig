package io.microconfig.configs.resolver.spel;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.PropertyResolveException;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.RootComponent;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;

/**
 * Resolves placeholders in format of spring EL
 *
 * @see SpelExpression
 */
@RequiredArgsConstructor
public class SpelExpressionResolver implements PropertyResolver {
    private final PropertyResolver delegate;

    @Override
    public String resolve(Property property, RootComponent root) {
        String resolvedPlaceholders = delegate.resolve(property, root);
        return resolveSpels(resolvedPlaceholders, root);
    }

    private String resolveSpels(String placeholders, RootComponent root) {
        StringBuilder currentValue = new StringBuilder(placeholders);
        while (true) {
            Matcher matcher = SpelExpression.PATTERN.matcher(currentValue.toString());
            if (!matcher.find()) break;

            String resolvedValue = doResolve(matcher.group(), root);
            currentValue.replace(matcher.start(), matcher.end(), resolvedValue);
        }

        return currentValue.toString();
    }

    private String doResolve(String value, RootComponent root) {
        SpelExpression expression = SpelExpression.parse(value);
        try {
            return expression.resolve();
        } catch (RuntimeException e) {
            throw new PropertyResolveException(expression, root, e);
        }
    }
}