package io.microconfig.service.fsgraph;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface FileSystemGraph {
    File getRootDir();

    Optional<File> getFolderOf(String component);

    Stream<File> getConfigFilesFor(String component, Predicate<File> filter);
}