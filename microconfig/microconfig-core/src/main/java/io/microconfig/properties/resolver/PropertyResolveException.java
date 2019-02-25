package io.microconfig.properties.resolver;

import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.spel.SpelExpression;

import static java.lang.String.format;

public class PropertyResolveException extends RuntimeException {
    public PropertyResolveException(String message) {
        super(message);
    }

    private PropertyResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyResolveException(String innerPlaceholder, Property sourceOfPlaceholder,
                                    RootComponent root, Throwable cause) {
        this(getMessage(innerPlaceholder, sourceOfPlaceholder, root), cause);
    }

    public PropertyResolveException(String innerPlaceholder, Property sourceOfPlaceholder,
                                    RootComponent root) {
        super(getMessage(innerPlaceholder, sourceOfPlaceholder, root));
    }

    public PropertyResolveException(SpelExpression expression, RootComponent root, Throwable cause) {
        this(format("Can't resolve spel: %s. Root component: %s[%s]. " +
                        "All string must be escaped with single quote '. " +
                        "Example of right spel: #{'${component1@ip}' + ':' + ${ports@port1}}",
                expression, root.getRootComponent().getName(), root.getRootEnv()), cause);
    }

    private static String getMessage(String innerPlaceholder, Property sourceOfPlaceholder, RootComponent root) {
        return format("Can't resolve placeholder: %s. Root component: %s[%s]. Source or error: %s[%s] -> %s:%d",
                innerPlaceholder, root.getRootComponent().getName(), root.getRootEnv(),
                sourceOfPlaceholder.getSource().getComponent().getName(), sourceOfPlaceholder.getEnvContext(),
                sourceOfPlaceholder.getSource().getSourceOfProperty(), sourceOfPlaceholder.getSource().getLine() + 1);
    }

    public static PropertyResolveException badPlaceholderFormat(String value) {
        return new PropertyResolveException("Can't asProperties placeholders: " + value + ". Supported format: ${componentName[optionalEnvName]@propertyPlaceholder:optionalDefaultValue}");
    }

    public static PropertyResolveException badSpellFormat(String value) {
        throw new PropertyResolveException(value + " is not spel expression. Supported format is: #{expression}");
    }
}
