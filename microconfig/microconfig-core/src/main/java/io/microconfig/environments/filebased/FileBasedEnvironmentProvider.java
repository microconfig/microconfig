package io.microconfig.environments.filebased;

import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.IoUtils.walk;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class FileBasedEnvironmentProvider implements EnvironmentProvider {
    private final File rootDirectory;
    private final EnvironmentParserSelector environmentParserSelector;

    public FileBasedEnvironmentProvider(File rootDirectory, EnvironmentParserSelector environmentParserSelector) {
        if (!rootDirectory.exists()) {
            throw new IllegalArgumentException("Can't find env directory '" + rootDirectory + "'. Maybe -Droot directory is incorrect.");
        }
        this.rootDirectory = rootDirectory;
        this.environmentParserSelector = environmentParserSelector;
    }

    @Override
    public Set<String> getEnvironmentNames() {
        try (Stream<File> envStream = envFiles(null)) {
            return envStream
                    .map(f -> f.getName().substring(0, f.getName().indexOf('.')))
                    .collect(toSet());
        }
    }

    @Override
    public Environment getByName(String name) {
        File envFile = getEnvFile(name);
        Environment environment = environmentParserSelector.selectParser(envFile)
                .parse(name, readFully(envFile))
                .processInclude(this);

        environment.verifyComponents();
        return environment;
    }

    private File getEnvFile(String name) {
        List<File> files;
        try (Stream<File> envStream = envFiles(name)) {
            files = envStream.collect(toList());
        }

        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        if (files.isEmpty()) {
            throw new EnvironmentNotExistException("Can't find env with name " + name);
        }
        return singleValue(files);
    }

    private Stream<File> envFiles(String envName) {
        List<String> supportedFormats = environmentParserSelector.supportedFormats();
        Predicate<File> fileNamePredicate = envName == null ?
                f -> supportedFormats.stream().anyMatch(format -> f.getName().endsWith(format))
                : f -> supportedFormats.stream().anyMatch(format -> f.getName().equals(envName + format));

        return walk(rootDirectory.toPath())
                .map(Path::toFile)
                .filter(fileNamePredicate);
    }
}
