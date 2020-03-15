package io.microconfig.domain.impl.environments.repository;

public class EnvironmentNotFoundException extends RuntimeException {
    public EnvironmentNotFoundException(String message) {
        super(message);
    }
}