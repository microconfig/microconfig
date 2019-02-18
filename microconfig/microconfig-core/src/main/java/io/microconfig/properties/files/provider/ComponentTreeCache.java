package io.microconfig.properties.files.provider;

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

import static io.microconfig.commands.factory.PropertyType.PROCESS;
import static io.microconfig.commands.factory.PropertyType.SERVICE;
import static io.microconfig.utils.IoUtils.walk;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;

@RequiredArgsConstructor
public class ComponentTreeCache implements ComponentTree {
    @Getter
    private final File repoDirRoot;
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

    private static Predicate<File> isDirectory() {
        return f -> {
            /*Filter by ext works way faster than File::isDirectory.
             Implementation is correct because File::listFiles for file will return null and we handle it in getPropertyFiles()
             */
            String name = f.getName();
            return !name.endsWith(SERVICE.getExtension())
                    && !name.endsWith(PROCESS.getExtension())
                    && !name.endsWith(".yaml");
        };
    }

    @Override
    public Stream<File> getPropertyFiles(String componentType, Predicate<File> filter) {
        return foldersByComponentType.getOrDefault(componentType, emptyList())
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