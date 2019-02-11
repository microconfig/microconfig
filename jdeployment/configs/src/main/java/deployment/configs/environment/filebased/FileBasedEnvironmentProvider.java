package deployment.configs.environment.filebased;

import deployment.configs.environment.Environment;
import deployment.configs.environment.EnvironmentNotExistException;
import deployment.configs.environment.EnvironmentParser;
import deployment.configs.environment.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static deployment.util.CollectionUtils.singleValue;
import static deployment.util.IoUtils.readFully;
import static deployment.util.IoUtils.walk;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class FileBasedEnvironmentProvider implements EnvironmentProvider {
    private final File rootDirectory;
    private final EnvironmentParser<String> environmentParser;

    public FileBasedEnvironmentProvider(File rootDirectory, EnvironmentParser<String> environmentParser) {
        if (!rootDirectory.exists()) {
            throw new IllegalArgumentException("Can't find env directory '" + rootDirectory + "'. Maybe -Droot directory is incorrect.");
        }
        this.rootDirectory = rootDirectory;
        this.environmentParser = environmentParser;
    }

    @Override
    public Set<String> getEnvironmentNames() {
        try (Stream<File> envStream = envFiles(empty())) {
            return envStream
                    .map(f -> f.getName().split("\\.")[0])
                    .collect(toSet());
        }
    }

    @Override
    public Environment getByName(String name) {
        Environment environment = environmentParser.parse(name, readFully(getEnvFile(name)))
                .processInclude(this);
        environment.verifyComponents();
        return environment;
    }

    private File getEnvFile(String name) {
        List<File> files;
        try (Stream<File> envStream = envFiles(of(name))) {
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

    private Stream<File> envFiles(Optional<String> envName) {
        String jsonExt = ".json";
        String yamlExt = ".yaml";

        Predicate<File> fileNamePredicate = envName.isPresent() ?
                f -> f.getName().equals(envName.get() + jsonExt) || f.getName().equals(envName.get() + yamlExt)
                : f -> f.getName().endsWith(jsonExt) || f.getName().endsWith(yamlExt);

        return walk(rootDirectory.toPath())
                .map(Path::toFile)
                .filter(fileNamePredicate);
    }
}
