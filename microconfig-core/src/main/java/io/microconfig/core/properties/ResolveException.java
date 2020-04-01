package io.microconfig.core.properties;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ResolveException extends RuntimeException {
    private final DeclaringComponent root;
    private final DeclaringComponent current;
    @Setter
    private Property property;

    public ResolveException(DeclaringComponent current, DeclaringComponent root, String message, Throwable cause) {
        super(message, cause);
        this.root = root;
        this.current = current;
        this.property = null;
    }

    @Override
    public String getMessage() {
        return componentInfo() + super.getMessage() + "\n" + getCauseMessage();
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

    private String getCauseMessage() {
        return ofNullable(getCause())
                .map(t -> ":" + t.getMessage())
                .orElse("");
    }
}
