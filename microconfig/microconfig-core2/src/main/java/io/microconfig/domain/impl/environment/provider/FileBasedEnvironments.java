package io.microconfig.domain.impl.environment.provider;


import io.microconfig.domain.Environment;
import io.microconfig.domain.Environments;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.io.FileUtils.walk;
import static io.microconfig.io.StreamUtils.map;
import static io.microconfig.io.formats.ConfigFormat.YAML;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileBasedEnvironments implements Environments {
    private final File envDir;
    private final EnvironmentParser parser;

    public FileBasedEnvironments(File envDir, EnvironmentParser parser) {
        this.envDir = envDir;
        this.parser = parser;

        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist " + envDir);
        }
    }

    @Override
    public List<Environment> all() {
        return map(envFiles(withYamlExtension()), f -> parser.parse(envName(f), f));
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
        return parser.parse(name, envFile(name));
//                .processInclude(this)
//                .verifyUniqueComponentNames();
    }

    @Override
    public Environment getOrCreateWithName(String name) {
        return withName(name);
    }

    private String envName(File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    private File envFile(String name) {
        List<File> files = envFiles(withFileName(name));

        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        if (files.isEmpty()) {
            throw new EnvironmentDoesNotExistException("Can't find env with name " + name);
        }
        return files.get(0);
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
}
