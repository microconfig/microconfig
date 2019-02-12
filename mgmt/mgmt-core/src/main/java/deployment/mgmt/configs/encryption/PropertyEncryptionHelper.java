package deployment.mgmt.configs.encryption;

import java.io.File;
import java.util.Properties;

import static deployment.util.FileUtils.write;
import static deployment.util.IoUtils.readFirstLine;
import static deployment.util.PropertiesUtils.loadProperties;
import static deployment.util.StringUtils.isEmpty;

public class PropertyEncryptionHelper {
    private static final String DEFAULT_SECRET_PROPERTY_MATCHER = "^.*password.*$";

    public static void secureProperties(String secretPropertiesPath,
                                        String pwdFilePath) {
        Properties properties = loadProperties(new File(secretPropertiesPath));
        String result = new PropertiesEncryptor(readPassword(pwdFilePath)).encryptProperties(properties, DEFAULT_SECRET_PROPERTY_MATCHER);
        write(new File(secretPropertiesPath), result);
    }

    public static String decryptProperty(String secretValue, String pwdFilePath) {
        return new PropertiesEncryptor(readPassword(pwdFilePath)).decrypt(secretValue);
    }

    private static String readPassword(String pwdFilePath) {
        File file = new File(pwdFilePath);
        String password = readFirstLine(file);
        if (isEmpty(password)) {
            throw new IllegalArgumentException("Password can't be empty. Pass file: " + file);
        }
        return password;
    }
}