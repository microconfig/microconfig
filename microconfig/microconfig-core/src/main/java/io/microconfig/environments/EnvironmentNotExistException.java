package io.microconfig.environments;

public class EnvironmentNotExistException extends RuntimeException {
    public EnvironmentNotExistException(String message) {
        super(message);
    }
}