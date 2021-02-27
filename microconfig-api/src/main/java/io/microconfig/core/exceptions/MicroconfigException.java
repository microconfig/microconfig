package io.microconfig.core.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MicroconfigException extends RuntimeException {
    public MicroconfigException(String message) {
        super(message);
    }

    public MicroconfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
