package io.microconfig.domain.impl.properties.repository;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ComponentGraph {
    File getRootDir();

    Optional<File> getFolderOf(String component);

    Stream<File> getConfigFilesFor(String component, Predicate<File> filter);
}