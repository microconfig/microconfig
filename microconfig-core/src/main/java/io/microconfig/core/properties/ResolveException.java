package io.microconfig.core.properties;

import lombok.Setter;

import static io.microconfig.utils.StringUtils.getCauseMessage;
import static java.util.Optional.ofNullable;

public class ResolveException extends RuntimeException {
    private final DeclaringComponent current;
    private final DeclaringComponent root;
    @Setter
    private Property property;

    public ResolveException(DeclaringComponent current, DeclaringComponent root, String message) {
        super(message);
        this.root = root;
        this.current = current;
        this.property = null;
    }

    public ResolveException(DeclaringComponent current, DeclaringComponent root, String message, Throwable cause) {
        super(message, cause);
        this.root = root;
        this.current = current;
        this.property = null;
    }

    @Override
    public String getMessage() {
        return componentInfo() + super.getMessage() + "\n" + getCauseMessage(this);
    }

    private String componentInfo() {
        return "Can't build configs for root component '" + root + "'.\n" +
                "Exception in '" + current + "'\n" +
                propertyMessage();
    }

    private String propertyMessage() {
        return ofNullable(property)
                .map(p -> "Property: " + p + "\n")
                .orElse("");
    }
}