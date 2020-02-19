package io.microconfig.core.environments;

public class EnvironmentNotExistException extends RuntimeException {
    public EnvironmentNotExistException(String message) {
        super(message);
    }
}