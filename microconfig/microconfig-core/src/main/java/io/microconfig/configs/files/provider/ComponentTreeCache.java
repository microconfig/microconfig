package io.microconfig.configs.files.provider;

import io.microconfig.commands.factory.StandardConfigType;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.IoUtils.walk;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;

@RequiredArgsConstructor
public class ComponentTreeCache implements ComponentTree {
    private final File configComponentsRoot;
    private final Map<String, List<File>> foldersByComponentType;

    public static ComponentTree build(File repoDirRoot) {
        try (Stream<Path> pathStream = walk(repoDirRoot.toPath()).parallel()) {
            Map<String, List<File>> cache = pathStream
                    .map(Path::toFile)
                    .filter(isDirectory())
                    .collect(groupingBy(File::getName));

            return new ComponentTreeCache(repoDirRoot, cache);
        }
    }

    @Override
    public File getConfigComponentsRoot() {
        return configComponentsRoot;
    }

    private static Predicate<File> isDirectory() {
        return f -> {
            /*Filter by ext works way faster than File::isDirectory.
             Implementation is correct because File::listFiles for file will return null and we handle it in getConfigFiles()
             */
            String name = f.getName();

            return Stream.of(StandardConfigType.values())
                    .flatMap(c -> c.getConfigExtensions().stream())
                    .noneMatch(name::endsWith);
        };
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
        return files.size() == 1 ? of(files.get(0)) : empty();
    }
}