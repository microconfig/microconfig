package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.io.StreamUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.microconfig.io.FileUtils.walk;
import static io.microconfig.io.formats.ConfigFormat.YAML;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileEnvironmentRepository implements EnvironmentRepository {
    private static final String ENV_DIR = "envs";

    private final File envDir;
    private final EnvironmentParser parser;

    public FileEnvironmentRepository(File rootDir, EnvironmentParser parser) {
        this.envDir = new File(rootDir, ENV_DIR);
        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist " + envDir);
        }

        this.parser = parser;
    }

    @Override
    public List<Environment> all() {
        return StreamUtils.forEach(envFiles(withYamlExtension()), f -> parser.parse(envName(f), f));
    }

    @Override
    public Set<String> environmentNames() {
        return envFiles(withYamlExtension())
                .stream()
                .map(this::envName)
                .collect(toCollection(TreeSet::new));
    }

    @Override
    public Environment withName(String name) {
        return findEnv(name).orElseThrow(() -> {
            throw new EnvironmentNotFoundException("Can't find env with name '" + name + "'");
        });
    }

    @Override
    public Environment getOrCreateWithName(String name) {
        return findEnv(name)
                .orElseGet(fakeEnvWithName(name));
    }

    private Optional<Environment> findEnv(String name) {
        return envFile(name)
                .map(envFile -> parser.parse(name, envFile));
        //                .processInclude(this)
//                .verifyUniqueComponentNames();
    }

    private Supplier<Environment> fakeEnvWithName(String name) {
        return () -> parser.fakeEnvWithName(name);
    }

    private Optional<File> envFile(String name) {
        List<File> files = envFiles(withFileName(name));
        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        return files.isEmpty() ? empty() : of(files.get(0));
    }

    private List<File> envFiles(Predicate<File> predicate) {
        try (Stream<Path> stream = walk(envDir.toPath())) {
            return stream
                    .map(Path::toFile)
                    .filter(predicate)
                    .collect(toList());
        }
    }

    private Predicate<File> withFileName(String envName) {
        return f -> f.getName().equals(envName + YAML.extension());
    }

    private Predicate<File> withYamlExtension() {
        return f -> f.getName().endsWith(YAML.extension());
    }

    private String envName(File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }
}
