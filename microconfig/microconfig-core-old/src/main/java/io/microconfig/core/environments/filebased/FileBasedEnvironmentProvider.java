package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentDoesNotExistException;
import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.core.properties.io.io.Io;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.walk;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileBasedEnvironmentProvider implements EnvironmentProvider {
    private final File envDir;
    private final EnvironmentParser parser;
    private final Io fileReader;

    public FileBasedEnvironmentProvider(File envDir, EnvironmentParser parser, Io fileReader) {
        this.envDir = envDir;
        this.parser = parser;
        this.fileReader = fileReader;

        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist " + envDir);
        }
    }

    @Override
    public Set<String> getEnvironmentNames() {
        try (Stream<File> envStream = envFiles(null)) {
            return envStream
                    .map(f -> f.getName().substring(0, f.getName().indexOf('.')))
                    .collect(toCollection(TreeSet::new));
        }
    }

    @Override
    public Environment getByName(String name) {
        File envFile = findEnvFile(name);

        return parser.parse(name, fileReader.readFully(envFile))
                .withSource(envFile)
                .processInclude(this)
                .verifyUniqueComponentNames();
    }

    private File findEnvFile(String name) {
        List<File> files = getEnvFiles(name);

        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        if (files.isEmpty()) {
            throw new EnvironmentDoesNotExistException("Can't find env with name " + name);
        }
        return files.get(0);
    }

    private List<File> getEnvFiles(String name) {
        try (Stream<File> envStream = envFiles(name)) {
            return envStream.collect(toList());
        }
    }

    private Stream<File> envFiles(String envName) {
        Predicate<File> fileNamePredicate = envName == null ?
                f -> f.getName().endsWith(".yaml") :
                f -> f.getName().equals(envName + ".yaml");

        return walk(envDir.toPath())
                .map(Path::toFile)
                .filter(fileNamePredicate);
    }
}
