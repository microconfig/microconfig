package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.getExtension;
import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.Logger.warn;
import static io.microconfig.utils.StringUtils.dotCountIn;
import static java.lang.Long.MIN_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class ConfigFileRepositoryImpl implements ConfigFileRepository {
    private static final String COMPONENTS_DIR = "components";

    private final Map<String, List<File>> foldersByComponentType;

    public static ConfigFileRepository traverseFrom(File rootDir) {
        File componentDir = new File(rootDir, COMPONENTS_DIR);
        if (!componentDir.exists()) {
            throw new IllegalArgumentException("Root directory must contain 'components' dir");
        }

        try (Stream<Path> paths = walk(componentDir.toPath())) {
            return new ConfigFileRepositoryImpl(collectFoldersByComponentType(paths));
        }
    }

    private static Map<String, List<File>> collectFoldersByComponentType(Stream<Path> pathStream) {
        return pathStream.parallel()
                .filter(Files::isDirectory)
                .map(Path::toFile)
                .collect(groupingBy(File::getName));
    }

    @Override //todo includes from diff components shouldn't be parsed multiple times
    public List<ConfigFile> getConfigFilesOf(String component, String environment, ConfigType configType) {
        List<File> dirs = foldersByComponentType.getOrDefault(component, emptyList());
        if (dirs.isEmpty()) {
            throw new ComponentNotFoundException(component);
        }

        return dirs.stream()
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Stream::of)
                .filter(configWith(configType).and(forEnv(environment)))
                .sorted(configPriority())
                .map(f -> new ConfigFile(f, configType.getName(), environment))
                .collect(toList());
    }

    private Predicate<File> configWith(ConfigType configType) {
        return f -> configType.getSourceExtensions().contains(getExtension(f));
    }

    private Predicate<File> forEnv(String environment) {
        return f -> dotCountIn(f.getName()) == 1 || f.getName().contains('.' + environment + '.');
    }

    private Comparator<File> configPriority() {
        return comparing(this::amountOfEnvironments)
                .thenComparing(File::getName);
    }

    private long amountOfEnvironments(File f) {
        long envCount = dotCountIn(f.getName()) - 1;
        if (envCount == 0) return MIN_VALUE;
        return -1 * envCount;
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