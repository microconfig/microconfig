package io.microconfig.core.environments.impl.repository;

public class EnvironmentException extends RuntimeException {
    public EnvironmentException(String message) {
        super(message);
    }

    public EnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}