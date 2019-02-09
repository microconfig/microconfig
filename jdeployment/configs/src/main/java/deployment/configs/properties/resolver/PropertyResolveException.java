package deployment.configs.properties.resolver;

import deployment.configs.properties.Property;
import deployment.configs.properties.resolver.spel.SpelExpression;

import static java.lang.String.format;

public class PropertyResolveException extends RuntimeException {
    public PropertyResolveException(String message) {
        super(message);
    }

    public PropertyResolveException(String message, Throwable cause) {
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
        this(format("Can't resolver spel: %s. Root component: %s[%s]. " +
                        "All string must be escaped with single quote '. " +
                        "Example of right spel: #{'${component1@ip}' + ':' + ${ports@port1}}",
                expression, root.getRootComponent().getName(), root.getRootComponentEnv()), cause);
    }

    private static String getMessage(String innerPlaceholder, Property sourceOfPlaceholder, RootComponent root) {
        return format("Can't resolver placeholder: %s. Root component: %s[%s]. Source or error: %s[%s] -> %s:%d",
                innerPlaceholder, root.getRootComponent().getName(), root.getRootComponentEnv(),
                sourceOfPlaceholder.getSource().getComponent().getName(), sourceOfPlaceholder.getEnvContext(),
                sourceOfPlaceholder.getSource().getSourceOfProperty(), sourceOfPlaceholder.getSource().getLine() + 1);
    }
}
