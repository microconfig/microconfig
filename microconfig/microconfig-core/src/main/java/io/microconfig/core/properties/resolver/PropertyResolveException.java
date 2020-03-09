package io.microconfig.core.properties.resolver;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.expression.Expression;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.lang.String.format;

public class PropertyResolveException extends RuntimeException {
    public PropertyResolveException(String message) {
        super(message);
    }

    public PropertyResolveException(String unresolvedPlaceholder, Property sourceOfPlaceholder, EnvComponent root,
                                    Throwable cause) {
        super(resolveExceptionMessage(unresolvedPlaceholder, sourceOfPlaceholder, root), cause);
    }

    public PropertyResolveException(String unresolvedPlaceholder, Property sourceOfPlaceholder, EnvComponent root) {
        super(resolveExceptionMessage(unresolvedPlaceholder, sourceOfPlaceholder, root));
    }

    public PropertyResolveException(Expression expression, EnvComponent root, Throwable cause) {
        super(format("Can't evaluate EL '%s'. Root component -> %s[%s]. " +
                        "All string must be escaped with single quote '. " +
                        "Example of correct EL: #{'${component1@ip}' + ':' + ${ports@port1}}",
                expression, root.getComponent().getName(), root.getEnvironment()), cause
        );
    }

    private static String resolveExceptionMessage(String unresolvedPlaceholder, Property sourceOfPlaceholder, EnvComponent root) {
        return format("Can't resolve placeholder '%s' defined in " + LINES_SEPARATOR + "'%s', that property is a transitive dependency of '%s'.",
                unresolvedPlaceholder,
                sourceOfPlaceholder.getSource().sourceInfo(),
                root);
    }

    public static PropertyResolveException badPlaceholderFormat(String value) {
        return new PropertyResolveException("Can't parse placeholder '" + value + "'"
                + ". Supported format: ${componentName[optionalEnvName]@propertyPlaceholder:optionalDefaultValue}");
    }

    public static PropertyResolveException badSpellFormat(String value) {
        throw new PropertyResolveException("'" + value + "' is not an expression. Supported format is: #{expression}");
    }
}