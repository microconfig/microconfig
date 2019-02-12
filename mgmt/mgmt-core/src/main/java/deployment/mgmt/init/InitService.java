package deployment.mgmt.init;

public interface InitService {
    void fullInit(boolean withoutStopping);

    void init(String configVersion, String projectFullVersionOrPostfix);

    void initWithoutStopping(String configVersion, String projectFullVersionOrPostfix);

    void fetchConfigs();
}