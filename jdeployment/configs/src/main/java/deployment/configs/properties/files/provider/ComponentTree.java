package deployment.configs.properties.files.provider;

import java.io.File;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ComponentTree {
    Stream<File> getPropertyFiles(String componentType, Predicate<File> filter);

    File getRepoDirRoot();
}