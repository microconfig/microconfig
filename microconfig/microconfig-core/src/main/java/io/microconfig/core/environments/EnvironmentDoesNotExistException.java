package io.microconfig.core.environments;

public class EnvironmentDoesNotExistException extends RuntimeException {
    public EnvironmentDoesNotExistException(String message) {
        super(message);
    }
}