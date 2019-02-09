package deployment.mgmt.process.start.prestart;

import deployment.mgmt.configs.filestructure.DeployFileStructureImpl;

public class ArchiveLogsTestIT {
    public static void main(String[] args) {
        new ArchiveLogs(DeployFileStructureImpl.init()).beforeStart("cr-feed-audit", null);
    }
}