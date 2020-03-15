package io.microconfig.domain.impl.environment.repository;

public class EnvironmentNotFoundException extends RuntimeException {
    public EnvironmentNotFoundException(String message) {
        super(message);
    }
}