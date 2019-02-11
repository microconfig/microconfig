package deployment.mgmt.configs.servicenameresolver;

public interface ServiceNameResolver {
    String ALL_SERVICE_ALIAS = "all";

    String[] resolve(String... serviceNamePatterns);

    String[] resolveWithoutTasks(String... serviceNamePatterns);

    String[] notStrictResolve(String... serviceNamePatterns);

    String resolveOne(String service);

    void requireCorrectName(String service);
}