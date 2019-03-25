package deployment.mgmt.configs.updateconfigs;

import deployment.mgmt.init.LegacyMgmtStructureImpl;
import io.microconfig.features.templates.RelativePathResolver;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

import static io.microconfig.utils.Logger.warn;

@RequiredArgsConstructor
public class OldConfigsRelativePathResolver implements RelativePathResolver {
    private final File repoRootDir;

    @Override
    public File overrideRelativePath(File serviceConfigDir, String path) {
        String prefix = "../../";

        Predicate<String> oldConfigPath = p -> {
            if (!path.startsWith(prefix)) return false;
            String nextDir = path.substring(prefix.length(), path.indexOf('/', prefix.length()));

            return new LegacyMgmtStructureImpl().containsDir(nextDir);
        };

        if (oldConfigPath.test(path)) {
            warn(warnMessage(path, serviceConfigDir));
            return new File(repoRootDir, path.substring(prefix.length()));
        }

        return new File(path);
    }

    private String warnMessage(String path, File serviceDir) {
        return "Overriding relative template path '../../' for " + serviceDir.getName() + "(" + path + ")."
                + " RELATIVE PATH IS DEPRECATED AND UNSUPPORTED. The support will be removed during next MGMT release."
                + " Please, replace relative path with absolute using ${this@configRoot}/.. or ${component_name@configDir}/.. ";
    }
}