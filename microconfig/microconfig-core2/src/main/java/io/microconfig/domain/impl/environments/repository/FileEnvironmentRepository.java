package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.domain.impl.environments.ComponentFactory;
import io.microconfig.domain.impl.environments.EnvironmentImpl;
import io.microconfig.io.FsReader;
import io.microconfig.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileEnvironmentRepository implements EnvironmentRepository {
    private static final String ENV_DIR = "envs";

    private final File envDir;
    private final FsReader fsReader;
    private final ComponentFactory componentFactory;

    public FileEnvironmentRepository(File rootDir, FsReader fsReader, ComponentFactory componentFactory) {
        this.envDir = new File(rootDir, ENV_DIR);
        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist: " + envDir);
        }
        this.fsReader = fsReader;
        this.componentFactory = componentFactory;
    }

    @Override
    public List<Environment> environments() {
        return forEach(environmentFiles(), parse());
    }

    @Override
    public Set<String> environmentNames() {
        return forEach(environmentFiles(), FileUtils::getName, toCollection(TreeSet::new));
    }

    @Override
    public Environment getWithName(String name) {
        return findEnvWith(name).orElseThrow(() -> {
            throw new EnvironmentException("Can't find env '" + name + "'");
        });
    }

    @Override
    public Environment getOrCreateWithName(String name) {
        return findEnvWith(name).orElseGet(fakeEnvWith(name));
    }

    private Optional<Environment> findEnvWith(String name) {
        return envFileWith(name).map(parse());
    }

    private Optional<File> envFileWith(String name) {
        List<File> envFiles = filter(environmentFiles(), withFileName(name));
        if (envFiles.size() > 1) {
            throw new EnvironmentException("Found several env files with name: " + name);
        }
        return envFiles.isEmpty() ? empty() : of(envFiles.get(0));
    }

    private List<File> environmentFiles() {
        try (Stream<Path> stream = walk(envDir.toPath())) {
            return stream.map(Path::toFile)
                    .filter(hasYamlExtension())
                    .collect(toList());
        }
    }

    private Function<File, Environment> parse() {
        return f -> new EnvironmentFile(f)
                .parseUsing(fsReader)
                .processInclude(this)
                .checkComponentNamesAreUnique()
                .toEnvironment(componentFactory);
    }

    private Supplier<Environment> fakeEnvWith(String name) {
        return () -> new EnvironmentImpl(name, emptyList(), componentFactory);
    }

    private Predicate<File> hasYamlExtension() {
        return f -> f.getName().endsWith(".yaml");
    }

    private Predicate<File> withFileName(String envName) {
        return f -> getName(f).equals(envName);
    }
}
