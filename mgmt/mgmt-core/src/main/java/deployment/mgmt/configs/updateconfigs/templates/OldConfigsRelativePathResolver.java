package deployment.mgmt.configs.updateconfigs.templates;

import deployment.mgmt.init.LegacyMgmtStructureImpl;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static deployment.util.Logger.warn;

@RequiredArgsConstructor
public class OldConfigsRelativePathResolver implements RelativePathResolver {
    private final File configRepoDir;

    @Override
    public File overrideRelativePath(String path, Supplier<String> warnMessage) {
        String prefix = "../../";

        Predicate<String> oldConfigPath = p -> {
            if (!path.startsWith(prefix)) return false;
            String nextDir = path.substring(prefix.length(), path.indexOf('/', prefix.length()));

            return new LegacyMgmtStructureImpl().containsDir(nextDir);
        };

        if (oldConfigPath.test(path)) {
            warn(warnMessage.get());
            return new File(configRepoDir, path.substring(prefix.length()));
        }

        return new File(path);
    }
}