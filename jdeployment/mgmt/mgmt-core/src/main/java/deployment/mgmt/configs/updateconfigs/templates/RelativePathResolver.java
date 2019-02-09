package deployment.mgmt.configs.updateconfigs.templates;

import java.io.File;
import java.util.function.Supplier;

public interface RelativePathResolver {
    File overrideRelativePath(String path, Supplier<String> warnMessage);
}
