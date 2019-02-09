package deployment.mgmt.configs.encryption;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;

import java.util.Properties;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static org.jasypt.properties.PropertyValueEncryptionUtils.isEncryptedValue;

public class PropertiesEncryptor {
    private final StringEncryptor encryptor;

    public PropertiesEncryptor(String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        this.encryptor = encryptor;
    }

    public String encryptProperties(Properties properties, String propertyMatcher) {
        Pattern pattern = Pattern.compile(propertyMatcher);

        return properties.entrySet().stream().map(e -> {
            String key = e.getKey().toString();
            String value = e.getValue().toString();
            return key + "=" + (pattern.matcher(key).matches() ? encrypt(value) : value);
        }).collect(joining("\n"));
    }

    public String encrypt(String value) {
        return isEncryptedValue(value) ? value : PropertyValueEncryptionUtils.encrypt(value, encryptor);
    }

    public String decrypt(String value) {
        return isEncryptedValue(value) ? PropertyValueEncryptionUtils.decrypt(value, encryptor) : value;
    }
}
