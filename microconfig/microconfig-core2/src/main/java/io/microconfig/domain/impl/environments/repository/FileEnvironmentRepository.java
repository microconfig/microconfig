package io.microconfig.domain.impl.environments.repository;

import io.microconfig.domain.Environment;
import io.microconfig.domain.EnvironmentRepository;
import io.microconfig.io.formats.Io;

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

import static io.microconfig.io.FileUtils.walk;
import static io.microconfig.io.StreamUtils.filter;
import static io.microconfig.io.StreamUtils.forEach;
import static io.microconfig.io.formats.ConfigFormat.YAML;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileEnvironmentRepository implements EnvironmentRepository {
    private static final String ENV_DIR = "envs";

    private final File envDir;
    private final Io io;

    public FileEnvironmentRepository(File rootDir, Io io) {
        this.envDir = new File(rootDir, ENV_DIR);
        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist: " + envDir);
        }
        this.io = io;
    }

    @Override
    public List<Environment> all() {
        return forEach(environmentFiles(), parse());
    }

    @Override
    public Set<String> environmentNames() {
        return forEach(environmentFiles(), getEnvName(), toCollection(TreeSet::new));
    }

    @Override
    public Environment withName(String name) {
        return findEnvWitH(name).orElseThrow(() -> {
            throw new EnvironmentNotFoundException("Can't find env with name '" + name + "'");
        });
    }

    @Override
    public Environment getOrCreateWithName(String name) {
        return findEnvWitH(name).orElseGet(fakeEnvWithName(name));
    }

    private Optional<Environment> findEnvWitH(String name) {
        return envFileWith(name).map(parse());
    }

    private Optional<File> envFileWith(String name) {
        List<File> envFiles = filter(environmentFiles(), withFileName(name));
        if (envFiles.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        return envFiles.isEmpty() ? empty() : of(envFiles.get(0));
    }

    private List<File> environmentFiles() {
        try (Stream<Path> stream = walk(envDir.toPath())) {
            return stream
                    .map(Path::toFile)
                    .filter(hasYamlExtension())
                    .collect(toList());
        }
    }

    private Function<File, Environment> parse() {
        return f -> {
            return null;
            //return new EnvironmentFile(f).parseUsing(io);
            //                .processInclude(this)
//                .verifyUniqueComponentNames();
        };
    }

    private Supplier<Environment> fakeEnvWithName(String name) {
//        return () -> parser.fakeEnvWithName(name);
        return null;
    }

    private Function<File, String> getEnvName() {
        return f -> {
            String name = f.getName();
            return name.substring(0, name.lastIndexOf('.'));
        };
    }

    private Predicate<File> hasYamlExtension() {
        return f -> f.getName().endsWith(YAML.extension());
    }

    private Predicate<File> withFileName(String envName) {
        return f -> f.getName().equals(envName + YAML.extension());
    }
}
