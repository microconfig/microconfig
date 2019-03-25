package io.microconfig.utils.reader;

import io.microconfig.utils.IoUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.IoUtils.lines;

public class FsFilesReader implements FilesReader {
    @Override
    public String read(File file) {
        return IoUtils.readFully(file);
    }

    @Override
    public List<String> readLines(File file) {
        return IoUtils.readLines(file);
    }

    @Override
    public Optional<String> firstLine(File file, Predicate<String> predicate) {
        try (Stream<String> lines = lines(file.toPath())) {
            return lines.filter(predicate).findFirst();
        }
    }
}