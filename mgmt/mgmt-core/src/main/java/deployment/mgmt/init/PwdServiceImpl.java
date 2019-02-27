package deployment.mgmt.init;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.IoUtils.firstLine;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.StringUtils.isEmpty;

@RequiredArgsConstructor
public class PwdServiceImpl implements PwdService {
    private final DeployFileStructure deployFileStructure;

    @Override
    public void createPwdFile(String env) {
        File encryptionKeyFile = deployFileStructure.deploy().getEncryptionKeyFile();
        if (!isEmpty(firstLine(encryptionKeyFile))) return;

        write(encryptionKeyFile, env);
        announce("Created default password file: " + encryptionKeyFile);
    }
}