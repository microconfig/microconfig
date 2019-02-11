package io.microconfig.environment;

public class EnvironmentNotExistException extends RuntimeException {
    public EnvironmentNotExistException(String message) {
        super(message);
    }
}