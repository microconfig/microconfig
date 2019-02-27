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
            warn(warnMessage(serviceConfigDir));
            return new File(repoRootDir, path.substring(prefix.length()));
        }

        return new File(path);
    }

    private String warnMessage(File serviceDir) {
        return "Overriding template path for " + serviceDir.getName() + " " + this +
                ". Use ${this@configDir}- resolves config root or ${component_name@folder} - resolves folder of config component";
    }
}