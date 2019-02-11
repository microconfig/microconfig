package deployment.mgmt.configs.encryption;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import static deployment.mgmt.configs.encryption.PropertyEncryptionHelper.decryptProperty;
import static deployment.mgmt.configs.encryption.PropertyEncryptionHelper.secureProperties;
import static deployment.util.Logger.announce;
import static deployment.util.TimeUtils.printLongTime;
import static deployment.util.TimeUtils.secAfter;
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