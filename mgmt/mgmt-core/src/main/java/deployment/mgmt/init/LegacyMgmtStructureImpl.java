package deployment.mgmt.init;

import deployment.util.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Set;

import static deployment.util.FileUtils.userHome;
import static java.util.stream.Stream.concat;

@RequiredArgsConstructor
public class LegacyMgmtStructureImpl implements LegacyMgmtStructure {
    private final Set<String> mgmtDirs = Set.of("bin", "bootstrap", "repo", "share", "ansible", "auto", ".git", "lib");
    private final Set<String> mgmtFiles = Set.of(".gitignore", "bootstrap.config", "bootstrap.sh", "winerr.log");

    @Override
    public boolean containsDir(String dir) {
        return mgmtDirs.contains(dir);
    }

    @Override
    public void deleteAll() {
        concat(mgmtDirs.stream(), mgmtFiles.stream())
                .map(d -> new File(userHome(), d))
                .forEach(FileUtils::delete);
    }
}