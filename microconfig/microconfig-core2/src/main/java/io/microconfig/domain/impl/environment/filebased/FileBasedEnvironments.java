package io.microconfig.domain.impl.environment.filebased;


import io.microconfig.domain.Environment;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static io.microconfig.utils.FileUtils.walk;
import static java.nio.file.Files.exists;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileBasedEnvironments implements Environments {
    private final Path envDir;
    private final EnvironmentParser parser;

    public FileBasedEnvironments(Path envDir, EnvironmentParser parser) {
        this.envDir = envDir;
        this.parser = parser;

        if (!exists(envDir)) {
            throw new IllegalArgumentException("Env directory doesn't exist " + envDir);
        }
    }

    @Override
    public Set<String> environmentNames() {
        try (Stream<File> envStream = envFilesStream(null)) {
            return envStream
                    .map(File::getName)
                    .map(name -> name.substring(0, name.lastIndexOf('.')))
                    .collect(toCollection(TreeSet::new));
        }
    }

    @Override
    public Environment byName(String name) {
        File envFile = getEnvFile(name);

        return parser.parse(name, envFile);
//                .processInclude(this)
//                .verifyUniqueComponentNames();
    }

    private File getEnvFile(String name) {
        List<File> files = envFiles(name);

        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        if (files.isEmpty()) {
            throw new EnvironmentDoesNotExistException("Can't find env with name " + name);
        }
        return singleValue(files);
    }

    private List<File> envFiles(String name) {
        try (Stream<File> envStream = envFilesStream(name)) {
            return envStream.collect(toList());
        }
    }

    private Stream<File> envFilesStream(String env) {
        Predicate<File> envNamePredicate = envFileNamePredicate(env);

        return walk(envDir)
                .map(Path::toFile)
                .filter(envNamePredicate);
    }

    private Predicate<File> envFileNamePredicate(String envName) {
        String extension = ".yaml";
        return envName == null ?
                f -> f.getName().endsWith(extension) :
                f -> f.getName().equals(envName + extension);
    }
}
