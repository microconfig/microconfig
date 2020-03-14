package io.microconfig.io.fsgraph;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.io.FileUtils.walk;
import static io.microconfig.io.Logger.warn;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;

@RequiredArgsConstructor
public class CachedFileSystemGraph implements FileSystemGraph {
    public static final String COMPONENTS_DIR = "components";

    @Getter
    private final File rootDir;
    private final Map<String, List<File>> foldersByComponentType;

    public static FileSystemGraph prepare(File rootDir) {
        if (!rootDir.exists()) {
            throw new IllegalArgumentException("Root directory doesn't exist: " + rootDir);
        }

        File components = new File(rootDir, COMPONENTS_DIR);
        if (!components.exists()) {
            throw new IllegalArgumentException("Root directory must contain 'components' dir");
        }

        try (Stream<Path> pathStream = walk(components.toPath())) {
            return new CachedFileSystemGraph(rootDir, collectFoldersByComponentType(pathStream));
        }
    }

    private static Map<String, List<File>> collectFoldersByComponentType(Stream<Path> pathStream) {
        return pathStream.parallel()
                .map(Path::toFile)
                .filter(isDirectory())
                .collect(groupingBy(File::getName));
    }

    @Override
    public Stream<File> getConfigFilesFor(String component, Predicate<File> filter) {
        return foldersByComponentType.getOrDefault(component, emptyList())
                .stream()
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Stream::of)
                .filter(filter);
    }

    @Override
    public Optional<File> getFolderOf(String component) {
        List<File> folders = foldersByComponentType.getOrDefault(component, emptyList());
        if (folders.size() > 1) {
            warn("Found " + folders.size() + " folders with name " + component + ". " +
                    "Consider renaming them, otherwise placeholder resolution can be incorrect");
        }
        return folders.isEmpty() ? empty() : of(folders.get(0));
    }

    private static Predicate<File> isDirectory() {
        return f -> {
            //Filter by ext works way faster than File::isDirectory.
            //Implementation is correct because File::listFiles for file will return null and we handle it in getConfigFiles()
            return !f.getName().contains(".");
        };
    }
}