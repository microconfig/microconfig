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
import java.util.stream.Stream;

import static deployment.util.CollectionUtils.singleValue;
import static deployment.util.IoUtils.readFully;
import static deployment.util.IoUtils.walk;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
public class FileBasedEnvironmentProvider implements EnvironmentProvider {
    private final File rootDirectory;
    private final String extension;
    private final EnvironmentParser<String> environmentParser;

    @Override
    public Set<String> getEnvironmentNames() {
        try (Stream<File> envStream = getEnvStream(empty())) {
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
        try (Stream<File> envStream = getEnvStream(of(name))) {
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

    private Stream<File> getEnvStream(Optional<String> envName) {
        return walk(rootDirectory.toPath())
                .map(Path::toFile)
                .filter(f -> {
                    String ext = "." + extension;
                    return envName.isPresent() ?
                            f.getName().equals(envName.get() + ext)
                            : f.getName().endsWith(ext);
                });
    }
}
