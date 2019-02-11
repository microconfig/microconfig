package deployment.mgmt.init;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static deployment.util.FileUtils.write;
import static deployment.util.IoUtils.readFirstLine;
import static deployment.util.Logger.announce;
import static deployment.util.StringUtils.isEmpty;

@RequiredArgsConstructor
public class PwdServiceImpl implements PwdService {
    private final DeployFileStructure deployFileStructure;

    @Override
    public void createPwdFile(String env) {
        File encryptionKeyFile = deployFileStructure.deploy().getEncryptionKeyFile();
        if (!isEmpty(readFirstLine(encryptionKeyFile))) return;

        write(encryptionKeyFile, env);
        announce("Created default password file: " + encryptionKeyFile);
    }
}