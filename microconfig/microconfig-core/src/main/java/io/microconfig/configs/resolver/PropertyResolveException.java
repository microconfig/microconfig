package io.microconfig.configs.resolver;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.expression.Expression;

import static java.lang.String.format;

public class PropertyResolveException extends RuntimeException {
    public PropertyResolveException(String message) {
        super(message);
    }

    private PropertyResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyResolveException(String innerPlaceholder, Property sourceOfPlaceholder,
                                    EnvComponent root, Throwable cause) {
        this(getMessage(innerPlaceholder, sourceOfPlaceholder, root), cause);
    }

    public PropertyResolveException(String innerPlaceholder, Property sourceOfPlaceholder,
                                    EnvComponent root) {
        super(getMessage(innerPlaceholder, sourceOfPlaceholder, root));
    }

    public PropertyResolveException(Expression expression, EnvComponent root, Throwable cause) {
        this(format("Can't resolve spel: %s. Root component: %s[%s]. " +
                        "All string must be escaped with single quote '. " +
                        "Example of right spel: #{'${component1@ip}' + ':' + ${ports@port1}}",
                expression, root.getComponent().getName(), root.getEnvironment()), cause);
    }

    private static String getMessage(String innerPlaceholder, Property sourceOfPlaceholder, EnvComponent root) {
        return format("Can't resolve placeholder: %s. Root component: %s[%s]. Source or error: %s[%s] '%s'",
                innerPlaceholder, root.getComponent().getName(), root.getEnvironment(),
                sourceOfPlaceholder.getSource().getComponent().getName(), sourceOfPlaceholder.getEnvContext(),
                sourceOfPlaceholder.getSource().sourceInfo());
    }

    public static PropertyResolveException badPlaceholderFormat(String value) {
        return new PropertyResolveException("Can't resolve placeholders: " + value + ". Supported format: ${componentName[optionalEnvName]@propertyPlaceholder:optionalDefaultValue}");
    }

    public static PropertyResolveException badSpellFormat(String value) {
        throw new PropertyResolveException(value + " is not spel expression. Supported format is: #{expression}");
    }
}
