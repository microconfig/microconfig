package io.microconfig.core.service.io.io;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface Io {
    String read(File file);

    List<String> readLines(File file);

    Optional<String> firstLine(File file, Predicate<String> predicate);
}