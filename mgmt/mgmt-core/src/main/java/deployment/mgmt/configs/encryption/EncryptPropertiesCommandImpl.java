package deployment.mgmt.configs.encryption;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import static deployment.mgmt.configs.encryption.PropertyEncryptionHelper.decryptProperty;
import static deployment.mgmt.configs.encryption.PropertyEncryptionHelper.secureProperties;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.TimeUtils.printLongTime;
import static io.microconfig.utils.TimeUtils.secAfter;
import static java.lang.System.currentTimeMillis;

@RequiredArgsConstructor
public class EncryptPropertiesCommandImpl implements EncryptPropertiesCommand {
    private final DeployFileStructure deployFileStructure;

    @Override
    public void encryptProperties() {
        announce("Encrypting secret properties...");

        long t = currentTimeMillis();
        secureProperties(
                deployFileStructure.deploy().getSecretPropertiesFile().getAbsolutePath(),
                deployFileStructure.deploy().getEncryptionKeyFile().getAbsolutePath()
        );

        announce("Encrypted secret properties in " + secAfter(t));
    }

    @Override
    public String decrypt(String encryptedValue) {
        return printLongTime(
                () -> decryptProperty(encryptedValue, deployFileStructure.deploy().getEncryptionKeyFile().getAbsolutePath()),
                "Decrypted secret properties"
        );
    }
}