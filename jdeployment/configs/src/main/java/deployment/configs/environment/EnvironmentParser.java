package deployment.configs.environment;

public interface EnvironmentParser<T> {
    Environment parse(String name, T content);
}
