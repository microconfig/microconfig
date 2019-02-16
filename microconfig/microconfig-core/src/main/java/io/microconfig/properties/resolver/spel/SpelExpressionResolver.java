package io.microconfig.properties.resolver.spel;

import io.microconfig.properties.Property;
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

        StringBuilder currentValue = new StringBuilder(resolvedPlaceholders);
        while (true) {
            Matcher matcher = SpelExpression.PATTERN.matcher(currentValue.toString());
            if (!matcher.find()) break;

            SpelExpression expression = SpelExpression.parse(matcher.group());
            String resolvedValue = expression.resolve(root);
            currentValue.replace(matcher.start(), matcher.end(), resolvedValue);
        }

        return currentValue.toString();
    }
}
