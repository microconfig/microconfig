package deployment.configs.properties.resolver.spel;

import deployment.configs.properties.Property;
import deployment.configs.properties.resolver.PropertyResolveException;
import deployment.configs.properties.resolver.PropertyResolver;
import deployment.configs.properties.resolver.RootComponent;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.regex.Matcher;

/**
 * Resolves placeholders in format of spring EL
 *
 * @see SpelExpression
 */
public class SpelExpressionResolver implements PropertyResolver {
    private final ExpressionParser parser = new SpelExpressionParser();
    private final PropertyResolver placeholderResolver;

    public SpelExpressionResolver(PropertyResolver placeholderResolver) {
        this.placeholderResolver = placeholderResolver;
    }

    @Override
    public String resolve(Property property, RootComponent root) {
        String resolvedPlaceholders = placeholderResolver.resolve(property, root);

        StringBuilder currentValue = new StringBuilder(resolvedPlaceholders);
        while (true) {
            Matcher matcher = SpelExpression.PATTERN.matcher(currentValue.toString());
            if (!matcher.find()) break;

            SpelExpression expression = SpelExpression.parse(matcher.group());
            String resolvedValue = resolveSpel(expression, root);
            currentValue.replace(matcher.start(), matcher.end(), resolvedValue);
        }

        return currentValue.toString();
    }

    private String resolveSpel(SpelExpression expression, RootComponent root) {
        try {
            return parser.parseExpression(expression.getValue()).getValue(String.class);
        } catch (RuntimeException e) {
            throw new PropertyResolveException(expression, root, e);
        }
    }
}
