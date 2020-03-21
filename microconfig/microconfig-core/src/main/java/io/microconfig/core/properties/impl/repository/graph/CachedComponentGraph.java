package io.microconfig.core.properties.impl.repository.graph;

import io.microconfig.core.properties.impl.repository.ComponentGraph;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.Logger.warn;
import static io.microconfig.utils.StringUtils.symbolCountIn;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class CachedComponentGraph implements ComponentGraph {
    public static final String COMPONENTS_DIR = "components";

    private final Map<String, List<File>> foldersByComponentType;

    public static ComponentGraph traverseFrom(File rootDir) {
        File componentDir = new File(rootDir, COMPONENTS_DIR);
        if (!componentDir.exists()) {
            throw new IllegalArgumentException("Root directory must contain 'components' dir");
        }

        try (Stream<Path> paths = walk(componentDir.toPath())) {
            return new CachedComponentGraph(collectFoldersByComponentType(paths));
        }
    }

    private static Map<String, List<File>> collectFoldersByComponentType(Stream<Path> pathStream) {
        return pathStream.parallel()
                .filter(Files::isDirectory)
                .map(Path::toFile)
                .collect(groupingBy(File::getName));
    }

    @Override
    public Stream<File> getConfigFilesFor(String component, Predicate<File> configFileFilter) {
        List<File> dirs = foldersByComponentType.getOrDefault(component, emptyList());
        if (dirs.isEmpty()) {
            throw new ComponentNotFoundException(component);
        }

        return dirs.stream()
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Stream::of)
                .filter(configFileFilter)
                .sorted(comparing(this::amountOfEnvironments).thenComparing(File::getName));
    }

    private long amountOfEnvironments(File f) {
        return -1 * symbolCountIn(f.getName(), '.');
    }

    @Override
    public Optional<File> getFolderOf(String component) {
        List<File> folders = foldersByComponentType.getOrDefault(component, emptyList());
        if (folders.size() > 1) {
            warn("Found " + folders.size() + " folders with name '" + component + "'. " +
                    "Consider renaming them, otherwise placeholder resolution works incorrectly");
        }
        return folders.isEmpty() ? empty() : of(folders.get(0));
    }
}