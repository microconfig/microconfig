package deployment.mgmt.configs.encryption;

public interface EncryptPropertiesCommand {
    void encryptProperties();

    String decrypt(String value);
}
