package io.microconfig.io;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface FsReader {
    String readFully(File file);

    List<String> readLines(File file);

    Optional<String> firstLineOf(File file, Predicate<String> predicate);
}