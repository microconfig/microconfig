package deployment.mgmt.configs.encryption;

import io.microconfig.configs.files.io.ConfigIoSelector;

import java.io.File;
import java.util.Map;

import static io.microconfig.utils.FileUtils.write;

public class PropertyEncryptionHelper {
    private static final String DEFAULT_SECRET_PROPERTY_MATCHER = "^.*password.*$";

    public static void encryptProperties(File propertiesFile, File passwordFile) {
        Map<String, String> properties = ConfigIoSelector.getInstance().read(propertiesFile);
        String result = new PropertiesEncryptor(passwordFile).encryptProperties(properties, DEFAULT_SECRET_PROPERTY_MATCHER);

        write(propertiesFile, result);
    }

    public static String decryptProperty(String secretValue, File passwordFile) {
        return new PropertiesEncryptor(passwordFile).decrypt(secretValue);
    }
}