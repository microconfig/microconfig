package deployment.mgmt.process.start.prestart;

import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.service.properties.ProcessProperties;
import deployment.mgmt.process.start.PreStartStep;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static deployment.util.FileUtils.createFile;
import static deployment.util.Logger.info;
import static deployment.util.ZipUtils.zip;

@RequiredArgsConstructor
public class ArchiveLogs implements PreStartStep {
    private static final String ARCHIVE_DIR = "archive";
    private final DeployFileStructure deployFileStructure;

    @Override
    public void beforeStart(String service, ProcessProperties ignore) {
        File logDir = deployFileStructure.logs().getLogDir(service);
        File[] logs = logDir.listFiles(File::isFile);
        if (logs.length == 0) return;

        info("Archiving old logs for " + service);
        File archiveFile = createFile(new File(logDir, ARCHIVE_DIR + "/" + new SimpleDateFormat("yyyy-MM-dd@HH-mm-ss").format(new Date()) + ".zip"));
        zip(logs, archiveFile);
    }
}