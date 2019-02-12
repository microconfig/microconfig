package deployment.mgmt.configs.deploysettings;

import deployment.mgmt.configs.encryption.PropertiesEncryptor;

import static io.microconfig.utils.StringUtils.isEmpty;
import static io.microconfig.utils.TimeUtils.printLongTime;

public class SimpleEncryptionServiceImpl implements EncryptionService {
    @Override
    public String encrypt(String value) {
        return printLongTime(() -> getPropertiesEncryptor().encrypt(value), "Encrypted value");
    }

    @Override
    public String decrypt(String value) {
        if (isEmpty(value)) return value;

        return printLongTime(() -> getPropertiesEncryptor().decrypt(value), "Decrypted value");
    }

    private PropertiesEncryptor getPropertiesEncryptor() {
        return new PropertiesEncryptor(String.valueOf("secret".hashCode()));
    }
}
