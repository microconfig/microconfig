package deployment.mgmt.configs.encryption;

public interface EncryptPropertiesCommand {
    void encryptSecretProperties();

    String decrypt(String value);
}
