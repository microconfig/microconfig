package io.microconfig.core.properties;

import static java.lang.String.format;

public class PropertyResolveException extends RuntimeException {
    public PropertyResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyResolveException(String message) {
        super(message);
    }

    public PropertyResolveException(String unresolvedValue, DeclaringComponent sourceOfPlaceholder, DeclaringComponent root,
                                    Throwable cause) {
        super(resolveExceptionMessage(unresolvedValue, sourceOfPlaceholder, root), cause);
    }

    public PropertyResolveException(String unresolvedValue, DeclaringComponent sourceOfPlaceholder, DeclaringComponent root) {
        super(resolveExceptionMessage(unresolvedValue, sourceOfPlaceholder, root));
    }

    public PropertyResolveException(String expression, DeclaringComponent root, Throwable cause) {
        super(format("Can't evaluate EL '%s'. Root component -> %s[%s]. " +
                        "Example of correct EL: #{'${component1@ip}' + ':' + ${ports@port1}}",
                expression, root.getComponent(), root.getEnvironment()), cause
        );
    }

    private static String resolveExceptionMessage(String unresolvedPlaceholder, DeclaringComponent sourceOfPlaceholder, DeclaringComponent root) {
        return format("Can't resolve placeholder '%s' defined in '%s'. That property is a transitive dependency of '%s'.",
                unresolvedPlaceholder,
//                sourceOfPlaceholder.getSource().sourceInfo(),
                sourceOfPlaceholder, //todo line number
                root
        );
    }

    public static PropertyResolveException badPlaceholderFormat(String value) {
        return new PropertyResolveException("Can't parse placeholder '" + value + "'"
                + ". Supported format: ${componentName[optionalEnvName]@propertyPlaceholder:optionalDefaultValue}");
    }

    public static PropertyResolveException badSpellFormat(String value) {
        throw new PropertyResolveException("'" + value + "' is not an expression. Supported format is: #{expression}");
    }
}
