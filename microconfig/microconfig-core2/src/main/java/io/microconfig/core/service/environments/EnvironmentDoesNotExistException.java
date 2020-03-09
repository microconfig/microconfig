package io.microconfig.core.service.environments;

public class EnvironmentDoesNotExistException extends RuntimeException {
    public EnvironmentDoesNotExistException(String message) {
        super(message);
    }
}