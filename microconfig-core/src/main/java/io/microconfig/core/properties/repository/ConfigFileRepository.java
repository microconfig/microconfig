package io.microconfig.core.properties.repository;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ConfigFileRepository {
    Stream<File> getConfigFilesFor(String component, Predicate<File> filter);

    Optional<File> getFolderOf(String component);
}