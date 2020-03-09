package io.microconfig.domain.impl.environment.filebased;

public class EnvironmentDoesNotExistException extends RuntimeException {
    public EnvironmentDoesNotExistException(String message) {
        super(message);
    }
}