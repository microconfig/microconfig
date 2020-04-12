package io.microconfig.core.environments.repository;

import static io.microconfig.utils.StringUtils.getCauseMessage;

public class EnvironmentException extends RuntimeException {
    public EnvironmentException(String message) {
        super(message);
    }

    public EnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + getCauseMessage(this);
    }
}