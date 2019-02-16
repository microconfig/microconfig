package io.microconfig.properties.resolver.spel;

import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.PropertyResolveException;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.RootComponent;
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

            String resolvedValue = doResolve(root, matcher);
            currentValue.replace(matcher.start(), matcher.end(), resolvedValue);
        }

        return currentValue.toString();
    }

    private String doResolve(RootComponent root, Matcher matcher) {
        SpelExpression expression = SpelExpression.parse(matcher.group());
        try {
            return expression.resolve();
        } catch (RuntimeException e) {
            throw new PropertyResolveException(expression, root, e);
        }
    }
}
