package deployment.mgmt.configs.deploysettings;

public interface EncryptionService {
    String encrypt(String value);

    String decrypt(String value);
}
