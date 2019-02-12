package deployment.mgmt.init;

import deployment.mgmt.configs.componentgroup.ComponentGroupService;
import deployment.mgmt.configs.filestructure.DeployFileStructure;
import deployment.mgmt.configs.filestructure.ProcessDirs;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.FileUtils.delete;

@RequiredArgsConstructor
public class OldFilesCleanerImpl implements OldFilesCleaner {
    private final ComponentGroupService componentGroupService;
    private final DeployFileStructure deployFileStructure;

    @Override
    public void deleteOldFiles() {
        deleteLegacyMgmtFiles();
        deleteNewMgmtFiles();
    }

    private void deleteLegacyMgmtFiles() {
        new LegacyMgmtStructureImpl().deleteAll();
    }

    private void deleteNewMgmtFiles() {
        componentGroupService.getServices().forEach(s -> {
            deletePid(s);
            deleteMgmtFilesExceptClasspath(s);
        });
    }

    private void deletePid(String service) {
        delete(deployFileStructure.service().getPidFile(service));
    }

    private void deleteMgmtFilesExceptClasspath(String service) {
        ProcessDirs processDirs = deployFileStructure.process();

        File classpathFile = processDirs.getClasspathFile(service);
        delete(processDirs.getProcessDir(service)
                .listFiles(f -> !f.getName().equals(classpathFile.getName()))
        );
    }
}
