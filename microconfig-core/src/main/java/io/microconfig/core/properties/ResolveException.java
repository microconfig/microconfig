package io.microconfig.core.properties;

import io.microconfig.core.exceptions.MicroconfigException;
import lombok.Setter;

import static io.microconfig.utils.StringUtils.getCauseMessage;
import static java.util.Optional.ofNullable;

public class ResolveException extends MicroconfigException {
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
        return rootComponentInfo() + problemInfo();
    }

    private String problemInfo() {
        return exceptionInfo() + super.getMessage() + "\n" + causeMessage();
    }

    private String rootComponentInfo() {
        return "Can't build configs for root component '" + root + "'.\n";
    }

    private String exceptionInfo() {
        return "Exception in\n" +
                "\t" + current + "'\n" +
                propertyMessage();
    }

    private String propertyMessage() {
        return ofNullable(property)
                .map(p -> "\t" + p + "\n")
                .orElse("");
    }

    private String causeMessage() {
        if (getCause() instanceof ResolveException) {
            return "Cause: " + ((ResolveException) getCause()).problemInfo();
        }
        return getCauseMessage(this);
    }
}