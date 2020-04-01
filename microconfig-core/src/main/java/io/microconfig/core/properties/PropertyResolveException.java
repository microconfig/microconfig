package io.microconfig.core.properties;

import static java.lang.String.format;

public class PropertyResolveException extends RuntimeException {
    public PropertyResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyResolveException(String message) {
        super(message);
    }


    public PropertyResolveException(String unresolvedValue, DeclaringComponent sourceOfPlaceholder, DeclaringComponent root) {
        super(resolveExceptionMessage(unresolvedValue, sourceOfPlaceholder, root));
    }

    private static String resolveExceptionMessage(String unresolvedPlaceholder, DeclaringComponent sourceOfPlaceholder, DeclaringComponent root) {
        return format("Can't resolve placeholder '%s' defined in '%s'. That property is a transitive dependency of '%s'.",
                unresolvedPlaceholder,
//                sourceOfPlaceholder.getSource().sourceInfo(),
                sourceOfPlaceholder, //todo line number
                root
        );
    }

}
