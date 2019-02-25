package deployment.mgmt.configs.encryption;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import io.microconfig.configs.files.io.ConfigIoService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.printLongTime;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.System.currentTimeMillis;

@RequiredArgsConstructor
public class EncryptPropertiesCommandImpl implements EncryptPropertiesCommand {
    private static final String DEFAULT_SECRET_PROPERTY_MATCHER = "^.*password.*$";
    private final DeployFileStructure deployFileStructure;
    private final ConfigIoService configIoService;

    @Override
    public void encryptSecretProperties() {
        announce("Encrypting secret properties...");

        long t = currentTimeMillis();
        encryptProperties(
                deployFileStructure.deploy().getSecretPropertiesFile(),
                deployFileStructure.deploy().getEncryptionKeyFile()
        );

        announce("Encrypted secret properties in " + secAfter(t));
    }

    @Override
    public String decrypt(String encryptedValue) {
        return printLongTime(
                () -> decryptProperty(encryptedValue, deployFileStructure.deploy().getEncryptionKeyFile()),
                "Decrypted secret properties"
        );
    }

    private void encryptProperties(File propertiesFile, File passwordFile) {
        Map<String, String> properties = configIoService.read(propertiesFile).propertiesAsMap();
        String result = new PropertiesEncryptor(passwordFile).encryptProperties(properties, DEFAULT_SECRET_PROPERTY_MATCHER);

        write(propertiesFile, result);
    }

    private String decryptProperty(String secretValue, File passwordFile) {
        return new PropertiesEncryptor(passwordFile).decrypt(secretValue);
    }
}