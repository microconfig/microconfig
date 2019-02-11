package io.microconfig.properties.files.provider;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ComponentTree {
    Stream<File> getPropertyFiles(String componentType, Predicate<File> filter);

    File getRepoDirRoot();

    Optional<File> getFolder(String component);
}