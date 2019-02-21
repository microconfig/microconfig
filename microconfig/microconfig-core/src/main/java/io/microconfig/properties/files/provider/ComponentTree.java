package io.microconfig.properties.files.provider;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ComponentTree {
    Stream<File> getConfigFiles(String component, Predicate<File> filter);

    File getConfigComponentsRoot();

    Optional<File> getFolder(String component);
}