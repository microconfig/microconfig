package io.microconfig.templates;

import java.io.File;
import java.util.function.Supplier;

public interface RelativePathResolver {
    File overrideRelativePath(String path, Supplier<String> warnMessage);

    static RelativePathResolver empty() {
        return (path, supplier) -> new File(path);
    }
}
