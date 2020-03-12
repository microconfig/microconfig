package io.microconfig.service.tree;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ComponentTree {
    File getRootDir();

    Optional<File> getFolderOf(String component);

    Stream<File> getConfigFilesFor(String component, Predicate<File> filter);
}