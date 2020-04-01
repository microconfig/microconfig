package io.microconfig.core.properties;

import lombok.RequiredArgsConstructor;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class ResolveException extends RuntimeException {
    private final DeclaringComponent root;
    private final DeclaringComponent current;

    public ResolveException(DeclaringComponent current, DeclaringComponent root, String message, Throwable cause) {
        super(message, cause);
        this.root = root;
        this.current = current;
    }

    @Override
    public String getMessage() {
        return componentInfo() + super.getMessage() + getCauseMessage();
    }

    private String componentInfo() {
        return "Can't build root component '" + root + "'.\nException in '" + current + "'.\n";
    }

    private String getCauseMessage() {
        return ofNullable(getCause())
                .map(Throwable::getMessage)
                .map(m -> ": " + m).orElse("");
    }
}
