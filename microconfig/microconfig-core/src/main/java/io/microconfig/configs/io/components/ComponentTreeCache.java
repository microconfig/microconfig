package io.microconfig.configs.io.components;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.walk;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;

@RequiredArgsConstructor
public class ComponentTreeCache implements ComponentTree {
    public static final String COMPONENTS_DIR = "components";

    private final File rootDir;
    private final Map<String, List<File>> foldersByComponentType;

    public static ComponentTree prepare(File rootDir) {
        if (!rootDir.exists()) {
            throw new IllegalArgumentException("Root directory doesnt exists: " + rootDir);
        }

        File components = new File(rootDir, COMPONENTS_DIR);
        if (!components.exists()) {
            throw new IllegalArgumentException("Root directory must contain 'components' dir");
        }

        try (Stream<Path> pathStream = walk(components.toPath())) {
            return new ComponentTreeCache(rootDir, filesByDirName(pathStream));
        }
    }

    private static Map<String, List<File>> filesByDirName(Stream<Path> pathStream) {
        return pathStream.parallel()
                .map(Path::toFile)
                .filter(isDirectory())
                .collect(groupingBy(File::getName));
    }

    @Override
    public File getRootDir() {
        return rootDir;
    }

    @Override
    public Stream<File> getConfigFiles(String component, Predicate<File> filter) {
        return foldersByComponentType.getOrDefault(component, emptyList())
                .stream()
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Stream::of)
                .filter(filter);
    }

    @Override
    public Optional<File> getFolder(String component) {
        List<File> files = foldersByComponentType.getOrDefault(component, emptyList());
        return files.size() == 1 ? Optional.of(files.get(0)) : empty();
    }

    private static Predicate<File> isDirectory() {
        return f -> {
            /*Filter by ext works way faster than File::isDirectory.
             Implementation is correct because File::listFiles for file will return null and we handle it in getConfigFiles()
             */
            return !f.getName().contains(".");
        };
    }
}