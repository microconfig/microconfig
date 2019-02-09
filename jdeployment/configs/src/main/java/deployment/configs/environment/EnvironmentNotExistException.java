package deployment.configs.environment;

public class EnvironmentNotExistException extends RuntimeException {
    public EnvironmentNotExistException(String message) {
        super(message);
    }
}