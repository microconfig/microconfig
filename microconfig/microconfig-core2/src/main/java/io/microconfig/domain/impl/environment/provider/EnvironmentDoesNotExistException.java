package io.microconfig.domain.impl.environment.provider;

public class EnvironmentDoesNotExistException extends RuntimeException {
    public EnvironmentDoesNotExistException(String message) {
        super(message);
    }
}