package io.microconfig.core.service.impl;

public class EnvironmentDoesNotExistException extends RuntimeException {
    public EnvironmentDoesNotExistException(String message) {
        super(message);
    }
}