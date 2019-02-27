package io.microconfig.environments.filebased;

import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.utils.reader.FileReader;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static io.microconfig.utils.FileUtils.walk;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class FileBasedEnvironmentProvider implements EnvironmentProvider {
    private final File envDir;
    private final EnvironmentParser environmentParser;
    private final FileReader fileReader;

    public FileBasedEnvironmentProvider(File envDir, EnvironmentParser environmentParser, FileReader fileReader) {
        this.envDir = envDir;
        this.environmentParser = environmentParser;
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
                    .collect(toSet());
        }
    }

    @Override
    public Environment getByName(String name) {
        File envFile = findEnvFile(name);

        return environmentParser
                .parse(name, fileReader.read(envFile))
                .processInclude(this)
                .verifyUniqueComponentNames();
    }

    private File findEnvFile(String name) {
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
        List<String> supportedFormats = environmentParser.supportedFormats();
        Predicate<File> fileNamePredicate = envName == null ?
                f -> supportedFormats.stream().anyMatch(format -> f.getName().endsWith(format))
                : f -> supportedFormats.stream().anyMatch(format -> f.getName().equals(envName + format));

        return walk(envDir.toPath())
                .map(Path::toFile)
                .filter(fileNamePredicate);
    }
}
